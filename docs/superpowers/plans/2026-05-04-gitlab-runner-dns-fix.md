# GitLab Runner DNS 解析问题修复实施计划

**设计文档**: `docs/superpowers/specs/2026-05-04-gitlab-runner-dns-fix-design.md`

## 概述

修复 GitLab Runner 创建的 CI 作业容器无法解析 `gitlab.harness.ai` 域名的问题，通过添加 Docker 网络别名和配置 Runner 网络模式实现。

## 前置条件

- Docker Compose 环境已部署
- GitLab 容器正常运行
- 有权限修改 docker-compose.yml 和注册脚本
- 有 Docker 执行权限

## 实施任务

### 任务 1: 修改 docker-compose.yml 添加网络别名

**目标**: 为 nginx 服务添加网络别名，使 devops 网络中的容器能够解析 `gitlab.harness.ai` 和 `registry.harness.ai`

**步骤**:
1. 读取当前 docker-compose.yml 中 nginx 服务的配置
   ```bash
   grep -A 15 "nginx:" deploy/docker-compose.yml
   ```

2. 修改 nginx 服务的 networks 配置，添加别名：
   ```yaml
   networks:
     devops:
       aliases:
         - gitlab.harness.ai
         - registry.harness.ai
   ```
   
   完整的 nginx 服务配置应该是：
   ```yaml
   nginx:
     image: nginx:alpine
     container_name: nginx
     restart: unless-stopped
     ports:
       - "80:80"
     volumes:
       - ./nginx/nginx.conf:/etc/nginx/nginx.conf:ro
     networks:
       devops:
         aliases:
           - gitlab.harness.ai
           - registry.harness.ai
     depends_on:
       - gitlab
   ```

3. 验证 YAML 语法
   ```bash
   python3 -c "import yaml; yaml.safe_load(open('deploy/docker-compose.yml'))" && echo "YAML valid"
   ```

**验证**: 
- YAML 语法正确
- nginx 服务的 networks 配置包含 aliases

**依赖**: 无

---

### 任务 2: 重启 nginx 容器应用配置

**目标**: 重启 nginx 容器使网络别名配置生效

**步骤**:
1. 进入 deploy 目录
   ```bash
   cd deploy
   ```

2. 重启 nginx 容器
   ```bash
   docker-compose up -d nginx
   ```

3. 检查容器状态
   ```bash
   docker ps | grep nginx
   ```

4. 检查容器日志
   ```bash
   docker logs nginx --tail 20
   ```

**验证**: 
- nginx 容器状态为 Up
- 无错误日志

**依赖**: 任务 1

---

### 任务 3: 验证网络别名解析

**目标**: 确认 devops 网络中的容器能够解析新添加的域名别名

**步骤**:
1. 在 lifecycle 容器中测试 DNS 解析
   ```bash
   docker exec lifecycle nslookup gitlab.harness.ai || docker exec lifecycle ping -c 1 gitlab.harness.ai
   ```

2. 测试 HTTP 访问
   ```bash
   docker exec lifecycle curl -I http://gitlab.harness.ai
   ```

3. 测试 registry 域名解析
   ```bash
   docker exec lifecycle nslookup registry.harness.ai || docker exec lifecycle ping -c 1 registry.harness.ai
   ```

**验证**: 
- 域名能够解析到 nginx 容器的 IP
- HTTP 请求返回 200 或 302 状态码
- registry 域名也能正常解析

**依赖**: 任务 2

---

### 任务 4: 修改 register-runner.sh 添加网络模式

**目标**: 修改 Runner 注册脚本，添加 `--docker-network-mode` 参数

**步骤**:
1. 读取当前注册脚本
   ```bash
   cat deploy/register-runner.sh | grep -A 10 "gitlab-runner register"
   ```

2. 在 `gitlab-runner register` 命令中添加 `--docker-network-mode "devops"` 参数
   
   修改后的命令应该是：
   ```bash
   docker exec gitlab-runner gitlab-runner register \
     --non-interactive \
     --url "${GITLAB_INTERNAL_URL}" \
     --token "${RUNNER_TOKEN}" \
     --executor docker \
     --docker-image alpine:latest \
     --docker-network-mode "devops" \
     --description "local-docker-runner" \
     --docker-volumes /var/run/docker.sock:/var/run/docker.sock
   ```

3. 验证脚本语法
   ```bash
   bash -n deploy/register-runner.sh && echo "Syntax OK"
   ```

**验证**: 
- 脚本包含 `--docker-network-mode "devops"` 参数
- Bash 语法正确

**依赖**: 任务 3

---

### 任务 5: 注销现有 Runner（如果存在）

**目标**: 注销现有 Runner 以便使用新配置重新注册

**步骤**:
1. 检查 Runner 状态
   ```bash
   docker exec gitlab-runner gitlab-runner list
   ```

2. 如果有 Runner 注册，则注销所有 Runner
   ```bash
   docker exec gitlab-runner gitlab-runner unregister --all-runners
   ```
   
   如果没有 Runner，跳过此步骤

3. 验证注销成功
   ```bash
   docker exec gitlab-runner gitlab-runner list
   ```

**验证**: 
- `gitlab-runner list` 显示没有注册的 Runner
- 或者显示 "Listing configured runners"但没有具体 Runner 信息

**依赖**: 任务 4

---

### 任务 6: 重新注册 Runner

**目标**: 使用修改后的脚本重新注册 Runner

**步骤**:
1. 确保在项目根目录
   ```bash
   pwd
   ```

2. 执行注册脚本
   ```bash
   cd deploy && ./register-runner.sh
   ```

3. 观察注册过程输出
   - 等待 GitLab 就绪
   - 创建 Personal Access Token
   - 创建 Runner token
   - 注册 Runner

4. 检查注册结果
   ```bash
   docker exec gitlab-runner gitlab-runner list
   ```

**验证**: 
- 脚本执行成功，输出 "Runner registered successfully"
- `gitlab-runner list` 显示已注册的 Runner
- Runner 描述为 "local-docker-runner"

**依赖**: 任务 5

---

### 任务 7: 验证 Runner 配置

**目标**: 确认 Runner 配置文件包含正确的网络模式设置

**步骤**:
1. 查看 Runner 配置文件
   ```bash
   docker exec gitlab-runner cat /etc/gitlab-runner/config.toml
   ```

2. 检查配置中是否包含 `network_mode = "devops"`
   ```bash
   docker exec gitlab-runner grep 'network_mode = "devops"' /etc/gitlab-runner/config.toml
   ```

3. 验证 Runner 状态
   ```bash
   docker exec gitlab-runner gitlab-runner verify
   ```

**验证**: 
- config.toml 包含 `network_mode = "devops"`
- Runner 状态为 "alive"
- 无错误信息

**依赖**: 任务 6

---

### 任务 8: 测试 CI Pipeline

**目标**: 触发 CI pipeline 验证作业容器能够解析域名并成功执行

**步骤**:
1. 提交一个小的更改触发 CI（或手动触发 pipeline）
   ```bash
   git commit --allow-empty -m "Test CI after runner DNS fix"
   git push
   ```

2. 在 GitLab UI 中观察 pipeline 执行
   - 访问 http://gitlab.harness.ai
   - 进入项目的 CI/CD > Pipelines
   - 查看最新 pipeline 的执行情况

3. 检查 CI 作业日志
   - 确认 git clone 操作成功
   - 确认没有 "Could not resolve host" 错误
   - 确认作业能够正常执行

**验证**: 
- Pipeline 成功执行
- 所有 jobs 状态为 passed
- 日志中无 DNS 解析错误

**依赖**: 任务 7

---

## 完成标准

- [ ] docker-compose.yml 中 nginx 服务包含网络别名
- [ ] devops 网络中的容器能够解析 gitlab.harness.ai 和 registry.harness.ai
- [ ] register-runner.sh 包含 --docker-network-mode "devops" 参数
- [ ] Runner 已重新注册
- [ ] Runner config.toml 包含 network_mode = "devops"
- [ ] CI pipeline 能够成功执行，无 DNS 解析错误

## 回滚方案

### 回滚网络别名配置

如果网络别名导致问题：

```bash
# 恢复 docker-compose.yml
git checkout deploy/docker-compose.yml
docker-compose up -d nginx
```

### 回滚 Runner 配置

如果 Runner 配置导致问题：

```bash
# 恢复注册脚本
git checkout deploy/register-runner.sh

# 注销 Runner
docker exec gitlab-runner gitlab-runner unregister --all-runners

# 使用原脚本重新注册
cd deploy && ./register-runner.sh
```

## 注意事项

1. **Docker 权限**: 需要有执行 docker 命令的权限
2. **GitLab 可用性**: 确保 GitLab 容器正常运行
3. **Runner token**: 重新注册需要 GitLab 生成新的 Runner token
4. **CI 作业影响**: 注销 Runner 期间无法执行 CI 作业
5. **网络别名作用域**: 别名只在 devops 网络内生效，宿主机仍需 /etc/hosts

## 预计时间

- 任务 1: 5 分钟
- 任务 2: 2 分钟
- 任务 3: 3 分钟
- 任务 4: 5 分钟
- 任务 5: 2 分钟
- 任务 6: 5 分钟（包括等待 GitLab 就绪）
- 任务 7: 2 分钟
- 任务 8: 5 分钟（取决于 pipeline 执行时间）
- **总计**: 约 30 分钟
