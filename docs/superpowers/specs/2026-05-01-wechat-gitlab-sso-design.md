# 微信扫码直接登录 GitLab SSO 设计

**日期：** 2026-05-01  
**适用范围：** harness-parent lifecycle 服务，本地 docker-compose 环境

---

## 一、目标

微信扫码后，用户无需输入用户名密码，直接进入 GitLab Web UI（建立原生 GitLab session）。

---

## 二、方案选择

采用 **方案 B1**：lifecycle 服务作为 OAuth2 Authorization Server，GitLab 通过 OmniAuth `oauth2_generic` 接入。

---

## 三、架构

### 3.1 登录流程

```
浏览器 → GitLab 点击"微信登录"
  → GitLab 302 → lifecycle:8081/oauth/authorize?client_id=gitlab-client&...
  → lifecycle 302 → /mock/wechat/authorize?redirect_uri=/oauth/wechat/callback&state=<oauth_state>
  → 用户点击授权 → GET /oauth/wechat/callback?code=mock_openid_001&state=<oauth_state>
  → lifecycle 用 openid 查/建 GitLab 用户，颁发 authorization_code
  → lifecycle 302 → GitLab redirect_uri?code=<auth_code>
  → GitLab POST lifecycle:8081/oauth/token（换 access_token）
  → GitLab GET lifecycle:8081/api/v4/user（获取用户信息）
  → GitLab 完成 SSO，建立原生 session
```

### 3.2 组件职责

| 组件 | 职责 |
|------|------|
| `AuthorizationServerConfig` | Spring Authorization Server 核心配置，注册 GitLab client |
| `WechatOAuthController` | 处理 `/oauth/authorize`（重定向微信）和 `/oauth/wechat/callback`（完成授权） |
| `WechatUserDetailsService` | openid → UserDetails，触发 getOrCreateGitlabUser |
| `GitLabUserInfoController` | `GET /api/v4/user`，返回 GitLab OmniAuth 要求的 userinfo 格式 |
| docker-compose lifecycle 服务 | Jib 构建镜像，加入 devops 网络 |
| GitLab OmniAuth 配置 | `GITLAB_OMNIBUS_CONFIG` 追加 oauth2_generic provider |

### 3.3 Session 绑定（内存）

`/oauth/authorize` 收到请求时，将完整的 `AuthorizationRequest`（含 `client_id`、`redirect_uri`、`state`、`scope`）存入 `ConcurrentHashMap<String, AuthorizationRequest>`，key 为随机 UUID（作为微信 state）。微信回调时用 state 取回，完成 OAuth2 授权码颁发。

---

## 四、新增/修改文件

```
harness-parent/gradle/libs.versions.toml          # 新增 spring-authorization-server 1.5.1、jib 插件
harness-parent/build.gradle                        # 新增 jib 插件声明

harness-common/auth-common/
└── dto/GitLabOAuthUserInfo.java                   # /api/v4/user 响应 record

harness-biz/lifecycle-biz/
├── build.gradle                                   # 新增 spring-authorization-server、spring-security 依赖
└── src/main/java/com/harness/lifecycle/
    ├── oauth/
    │   ├── AuthorizationServerConfig.java         # SAS 配置：RegisteredClient、SecurityFilterChain
    │   ├── WechatOAuthController.java             # /oauth/authorize + /oauth/wechat/callback
    │   ├── WechatUserDetailsService.java          # UserDetailsService 实现
    │   └── GitLabUserInfoController.java          # GET /api/v4/user
    └── config/
        └── SecurityConfig.java                    # 放行 /mock/**、/api/v4/user

infra/local-devops/
├── docker-compose.yml                             # 新增 lifecycle 服务，GitLab OmniAuth 配置
└── .env                                           # 新增 LIFECYCLE_IMAGE
```

---

## 五、关键配置

### 5.1 GitLab OmniAuth（追加到 GITLAB_OMNIBUS_CONFIG）

```ruby
gitlab_rails['omniauth_enabled'] = true
gitlab_rails['omniauth_allow_single_sign_on'] = ['oauth2_generic']
gitlab_rails['omniauth_block_auto_created_users'] = false
gitlab_rails['omniauth_providers'] = [
  {
    name: 'oauth2_generic',
    app_id: 'gitlab-client',
    app_secret: 'gitlab-secret',
    args: {
      client_options: {
        site: 'http://lifecycle:8081',
        authorize_url: '/oauth/authorize',
        token_url: '/oauth/token',
        user_info_url: '/api/v4/user'
      },
      user_response_structure: {
        id_path: 'id',
        attributes: { name: 'name', email: 'email', nickname: 'username' }
      }
    }
  }
]
```

### 5.2 Spring Authorization Server RegisteredClient

```
client_id:     gitlab-client
client_secret: gitlab-secret（BCrypt 编码）
grant_type:    authorization_code
redirect_uri:  http://localhost:8080/users/auth/oauth2_generic/callback
scope:         openid, profile, email
```

---

## 六、依赖版本

| 依赖 | 版本 |
|------|------|
| spring-security-oauth2-authorization-server | 1.5.1 |
| jib-gradle-plugin | 3.4.4 |

---

## 七、测试策略

- `AuthorizationServerConfigTest`：验证 RegisteredClient 注册正确
- `WechatOAuthControllerTest`：验证 `/oauth/authorize` 重定向到微信 Mock，`/oauth/wechat/callback` 完成授权码颁发
- `GitLabUserInfoControllerTest`：验证 `/api/v4/user` 返回正确格式
- 集成验证：docker-compose up → 浏览器访问 GitLab → 点击微信登录 → 直接进入 GitLab
