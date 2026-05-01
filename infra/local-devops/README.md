# 本地 DevOps 环境

GitLab + GitLab Runner，对应第三章 DevOps 反馈环实践。

## 前置条件

- Docker Desktop（建议分配 6GB+ 内存）
- 端口 8080、2222 未被占用

## 启动

```bash
# 1. 启动服务
docker-compose up -d

# 2. 等待 GitLab 就绪（约 2-3 分钟），然后注册 Runner
./register-runner.sh
```

## 验证

- Web UI：http://localhost:8080（用户名 `root`，密码见 `.env`）
- Runner 状态：Admin → CI/CD → Runners

## 示例 .gitlab-ci.yml

```yaml
stages:
  - build
  - test

build:
  stage: build
  tags:
    - local
  script:
    - echo "Building..."

test:
  stage: test
  tags:
    - local
  script:
    - echo "Testing..."
```

## 停止

```bash
docker-compose down
```

数据持久化在 `gitlab/` 和 `runner/` 目录，重启不丢失。
