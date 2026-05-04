# Harness — 本地 DevOps 全栈实践平台

基于 **Spring Boot 4.0 + Java 25 + Docker Compose** 的本地 DevOps 平台，涵盖微信 OAuth2 统一登录、GitLab 自动化用户管理、Apollo 配置中心、CI/CD 流水线及私有镜像仓库。

---

## 技术栈

![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.0-brightgreen)
![Java](https://img.shields.io/badge/Java-25-orange)
![Gradle](https://img.shields.io/badge/Gradle-8.x-blue)
![MyBatis-Plus](https://img.shields.io/badge/MyBatis--Plus-3.5.16-blue)
![MySQL](https://img.shields.io/badge/MySQL-8.0-blue)
![Docker](https://img.shields.io/badge/Docker-Compose-blue)
![Apollo](https://img.shields.io/badge/Apollo-Config-brightgreen)

---

## 30 秒快速启动

```bash
cd deploy && docker compose up -d && ./register-runner.sh
```

| 服务 | 地址 | 默认账号 |
|------|------|----------|
| GitLab | http://gitlab.harness.ai | `root` / 见 `.env` |
| Lifecycle | http://lifecycle.harness.ai:8081 | - |
| Apollo Portal | http://apollo.harness.ai:8070 | `apollo` / `admin` |
| Registry | http://registry.harness.ai | `admin` / `admin` |

> 首次启动需先配置 hosts，详见 [部署指南](docs/deployment.md)。

---

## 文档导航

| 文档 | 内容 | 面向读者 |
|------|------|----------|
| [开发指南](docs/development.md) | 代码结构、模块说明、本地运行、构建镜像、质量门禁 | 开发者 |
| [部署与运维](docs/deployment.md) | 服务架构、Docker Compose 部署、配置说明、故障排查 | 运维 / 开发者 |
| [架构与流程](docs/architecture.md) | 模块依赖、OAuth2 登录流程、GitLab 集成、架构守护规则 | 全栈 / 新成员 |

---

## 项目全景

```
┌─────────────────────────────────────────────────────────┐
│  Nginx (统一入口 :80)                                    │
│  ├── gitlab.harness.ai  → GitLab CE (:8080)             │
│  └── (lifecycle/registry/apollo 直连或预留扩展)          │
├─────────────────────────────────────────────────────────┤
│  Docker Compose 网络 (devops)                           │
│  ├── GitLab CE        代码托管 & CI/CD                  │
│  ├── GitLab Runner    CI/CD 执行器                      │
│  ├── Lifecycle        Spring Boot 业务服务 (:8081)      │
│  ├── Apollo           配置中心 (:8070/8080/8090)        │
│  ├── MySQL 8.0        共享数据库 (:3306)                │
│  └── Registry 2       私有镜像仓库 (:5000)              │
└─────────────────────────────────────────────────────────┘
```

---

## License

MIT
