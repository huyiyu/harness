## ADDED Requirements

### Requirement: GitHub Actions workflow 文件存在并发布到 Pages

系统 SHALL 在仓库 `.github/workflows/hugo.yml` 提供一个 GitHub Actions workflow,使每次推送默认分支后自动构建 Hugo 站点并通过官方 `actions/deploy-pages` 链路发布到 GitHub Pages。

#### Scenario: workflow 触发条件限定为 main 与手工

- **WHEN** 检查 `.github/workflows/hugo.yml`
- **THEN** 文件 `on` 段只包含 `push` 到 `main` 分支与 `workflow_dispatch`,不监听 `pull_request`、`schedule`、其他分支

#### Scenario: workflow 提交后首次推送可成功部署

- **WHEN** 用户已完成"创建 GitHub 仓库"和"Pages source 设置为 GitHub Actions"两项手工前置
- **AND** 本地 `git push -u origin main` 推送默认分支
- **THEN** GitHub Actions 自动启动该 workflow,且在 ≤ 5 分钟内完成 build + deploy 两个 job,最终状态为 success

#### Scenario: workflow 检出包含 submodule

- **WHEN** 检查 workflow 中的 `actions/checkout` 步骤
- **THEN** 该步骤参数包含 `submodules: recursive` 与 `fetch-depth: 0`

#### Scenario: workflow 使用 Hugo extended 且版本与本地一致

- **WHEN** 检查 workflow 中的 Hugo 安装步骤
- **THEN** 该步骤显式声明使用 Hugo extended,且 `hugo-version` 字段值等于本地开发使用的版本(本次锁定为 `0.160.0`)

#### Scenario: workflow 构建命令与 make build 行为一致

- **WHEN** 检查 workflow 中的构建步骤
- **THEN** 该步骤工作目录为 `docs/`,执行命令为 `hugo --minify --gc`,与本地 `make build` 完全一致

### Requirement: workflow 使用最小权限

系统 SHALL 为 workflow 配置最小必要权限:`contents: read`、`pages: write`、`id-token: write`,而不使用默认的写权限或 PAT。

#### Scenario: permissions 字段显式声明

- **WHEN** 检查 `.github/workflows/hugo.yml`
- **THEN** 顶层或 job 级别存在 `permissions` 段,值精确为 `contents: read`、`pages: write`、`id-token: write` 三项,不包含其他权限

### Requirement: 部署目标与 baseURL 一致

系统 SHALL 使部署后访问 `https://huyiyu.github.io/` 即得到与本地 `make preview` 视觉一致的中文文档站点(目录树展示 14 章 + 附录共 15 个章节占位)。

#### Scenario: 线上根路径返回首页且左侧目录可见

- **WHEN** 首次 workflow 部署成功后
- **AND** 浏览器访问 `https://huyiyu.github.io/`
- **THEN** 页面成功加载(HTTP 200),左侧目录树展示 14 章 + 附录共 15 个章节标题(中文),CSS / 字体 / JS 资源全部从 `https://huyiyu.github.io/` 域名下加载,无 404、无 mixed-content 警告

### Requirement: Pages 配置依赖手工前置步骤

系统 SHALL 在 `tasks.md` 中明确列出以下三项必须由用户在 GitHub 网页端完成的手工步骤,不允许通过自动化脚本绕过:

1. 创建 `huyiyu/harnness.github.io` 仓库(或在用户 `huyiyu` 名下创建同名仓库)。
2. 将本地仓库 `git remote add origin` 到该仓库。
3. 在 Settings → Pages 中将 Source 设置为 "GitHub Actions"。

这些步骤完成前,workflow 即使被推送也无法完成部署。

#### Scenario: tasks.md 显式标记人工步骤

- **WHEN** 检查 `tasks.md` 中与部署相关的任务
- **THEN** 上述三项各自有独立任务条目,标题或描述包含"手工"、"用户"或"GitHub 网页"等字样,且明确说明 Claude 在该步骤会停顿等待用户

### Requirement: 暂不引入除 Pages 之外的部署目标

系统 SHALL NOT 在本次 change 中引入 Netlify、Vercel、自建对象存储、Docker 镜像等额外部署目标的配置文件。

#### Scenario: 仓库无其他部署平台配置

- **WHEN** 检查仓库根
- **THEN** 不存在 `netlify.toml`、`vercel.json`、`Dockerfile`、`fly.toml`、`render.yaml` 等其他部署平台的配置文件
