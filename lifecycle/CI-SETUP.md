# GitLab CI 配置说明

## 前置条件

在 GitLab 项目设置中配置以下 CI/CD Variables：

1. 进入项目 Settings → CI/CD → Variables
2. 添加以下变量：

| 变量名 | 值 | 类型 |
|-------|-----|------|
| REGISTRY_URL | registry.harness.ai | Variable |
| REGISTRY_USERNAME | admin | Variable |
| REGISTRY_PASSWORD | (仓库密码) | Variable (Masked) |

## Pipeline 说明

- **Merge Request**: 执行 validate + test 阶段
- **Main 分支**: 执行完整流程（validate + test + build）
- **标签推送**: 执行完整流程（validate + test + build）

## 镜像标签

- Main 分支: `latest` + `<commit-hash>`
- 标签: `<tag-name>`
