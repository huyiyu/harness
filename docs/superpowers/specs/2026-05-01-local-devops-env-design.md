# 本地 DevOps 环境设计

**日期：** 2026-05-01  
**范围：** 代码托管 + CI（不含 CD）  
**位置：** `infra/local-devops/`

## 目标

为 Harness 工程实践文档搭建配套的本地 DevOps 环境，对应第三章"DevOps 反馈环"内容，提供可运行的 GitLab + GitLab CI 平台。

## 架构

### 服务组成

| 服务 | 镜像 | 用途 |
|------|------|------|
| gitlab | gitlab/gitlab-ce | 代码托管、Merge Request、CI 配置 |
| gitlab-runner | gitlab/gitlab-runner | CI job 执行器 |

### 网络

两个服务共享 `devops` bridge network，Runner 通过服务名 `gitlab` 访问 GitLab API。

### 目录结构

```
infra/local-devops/
├── docker-compose.yml
├── .env
├── gitlab/
│   └── config/          # GitLab 持久化（config/logs/data）
├── runner/
│   └── config/          # config.toml（注册后生成）
└── register-runner.sh   # 一次性注册脚本
```

## 关键设计决策

**Runner executor：Docker**  
Runner 挂载宿主机 `/var/run/docker.sock`，CI job 在独立容器内运行。比 privileged DinD 更安全，比 shell executor 隔离性更好。

**Runner 注册：一次性脚本**  
`register-runner.sh` 在首次 `docker-compose up` 后手动执行一次，配置写入 `runner/config/config.toml` 并持久化，后续重启无需重新注册。

**端口映射**

| 端口 | 用途 |
|------|------|
| 8080 | GitLab Web UI / API |
| 2222 | GitLab SSH（git clone） |

## CI Pipeline 数据流

```
push 代码
  → GitLab 检测 .gitlab-ci.yml
  → 通知 Runner 有新 job
  → Runner 启动 Docker 容器执行脚本
  → 结果回报 GitLab → Pipeline 状态更新
```

对应第三章"CI 构建 → 自动化测试"环节。

## 不在范围内

- Container Registry
- CD / 自动部署
- Prometheus / Grafana 监控（第四章范围）
