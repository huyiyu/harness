## 1. 仓库基础设施

- [x] 1.1 在 `/home/huyiyu/Documents/harnness` 执行 `git init`,默认分支命名为 `main`
- [x] 1.2 创建根级 `.gitignore`,排除 `docs/public/`、`docs/resources/_gen/`、`.hugo_build.lock`、`node_modules/`、操作系统临时文件
- [x] 1.3 创建根级 `.gitattributes`,声明 `* text=auto eol=lf`,并对 `*.png`、`*.jpg`、`*.ico` 标记 `binary`
- [x] 1.4 提交首个 commit:`chore: init empty repo`

## 2. Hugo 站点骨架

- [x] 2.1 在 `docs/` 下创建空目录:`content/`、`archetypes/`、`static/`、`layouts/`、`data/`,每个目录放一个 `.gitkeep`(`static/`、`layouts/`、`data/` 在本次 change 内只占位,后续按需填充)
- [x] 2.2 写入 `docs/hugo.toml`,包含:
  - `baseURL = "https://harnness.github.io/"`
  - `languageCode = "zh-cn"`
  - `defaultContentLanguage = "zh-cn"`
  - `title = "Harness 工程落地方案"`
  - `theme = "hugo-book"`
  - `enableGitInfo = true`
  - `[params]` 段开启 `BookSearch = true`、`BookToC = true`、`BookSection = "docs"`(若适用)、`BookRepo` 留空字符串
  - `[markup.goldmark.renderer]` 段 `unsafe = true`(允许 markdown 内嵌 HTML,Book 主题某些 shortcode 需要)
- [x] 2.3 写入 `docs/archetypes/default.md`,front matter 包含 `title`、`weight`(空字符串占位)、`bookCollapseSection = false`、`draft = true` 四项,正文为空
- [x] 2.4 写入 `docs/archetypes/section.md`(`hugo new` 生成 `_index.md` 时使用),front matter `title`、`weight`、`bookCollapseSection = true`、`draft = false`
- [x] 2.5 提交 commit:`feat(docs): scaffold hugo site config and archetypes`

## 3. Hugo Book 主题集成

- [x] 3.1 查询 `https://github.com/alex-shpak/hugo-book/releases` 当前最新 stable tag(本次实施时锁定;若无 release 则取 master 上 ≥ 30 天的稳定 commit)并记录到 commit message
- [x] 3.2 执行 `git submodule add https://github.com/alex-shpak/hugo-book.git docs/themes/hugo-book`
- [x] 3.3 在 submodule 工作树中 `git -C docs/themes/hugo-book checkout <tag>`,回到上层 `git add .gitmodules docs/themes/hugo-book`
- [x] 3.4 验证:`git submodule status` 输出无 `+` 或 `-` 前缀,commit hash 与 3.1 记录一致
- [x] 3.5 提交 commit:`feat(docs): integrate hugo-book theme as submodule (pinned to v13)`

## 4. 章节大纲落地

- [x] 4.1 在 `docs/content/` 下批量创建 14 个章节目录 + `99-appendix`,采用"上篇理论(01-07)+ 下篇实践(08-14)+ 附录"的双篇结构。上篇聚焦 Harness 核心思想、DevOps/SRE 理论体系融合、人机协作范式与闭环框架;下篇聚焦全生命周期各阶段的 AI 主导执行与人类验收推进机制。目录清单如下:
  - 上篇·理论指导思想:
    - `01-background` — 背景与 Harness 定义
    - `02-harness-core` — Harness 核心思想：评估驱动的自主 Agent 闭环
    - `03-devops-foundation` — DevOps 与持续交付理论
    - `04-sre-foundation` — SRE 与可靠性工程理论
    - `05-collaboration-paradigm` — 人机协作范式：验收驱动模型
    - `06-closed-loop-framework` — 全生命周期闭环框架设计
    - `07-agent-roles` — Agent 角色体系与分工模型
  - 下篇·落地实践路径:
    - `08-overall-architecture` — 总体架构：AI 执行引擎与人类验收层
    - `09-requirement-phase` — 需求阶段：AI 主导需求工程
    - `10-design-phase` — 架构设计阶段：AI 主导技术方案
    - `11-development-phase` — 研发阶段：AI 主导编码实现
    - `12-testing-phase` — 测试阶段：AI 主导质量验证
    - `13-deployment-ops-phase` — 部署与运维阶段：AI 主导系统保障
    - `14-review-evolution-phase` — 归档复盘与 AI 持续进化
  - 附录:
    - `99-appendix` — 附录
- [x] 4.2 为每个章节目录写入 `_index.md`,front matter 字段:
  - `title`:对应中文章节标题(详见 specs/harness-plan-outline/spec.md 第二条 Requirement)
  - `weight`:章节序号 × 10(`01-background → 10`,`02-harness-core → 20`,...,`14-review-evolution-phase → 140`,`99-appendix → 990`)
  - `bookCollapseSection = true`
  - `draft = false`
- [x] 4.3 每个 `_index.md` 正文部分仅写一行 `<!-- TODO: 在后续 change 中填充本章正文 -->`
- [x] 4.4 验证:`ls -1 docs/content/docs/` 输出 15 行(14 章 + 附录),顺序与上方一致
- [x] 4.5 提交 commit:`feat(docs): scaffold 14-chapter outline placeholders`

## 5. 本地预览入口

- [x] 5.1 写入根级 `Makefile`,包含目标 `preview`、`build`、`clean`、`check-hugo`,每个目标的实现要点:
  - `check-hugo`:`hugo version | grep -q extended || (echo '需要 Hugo extended 版本'; exit 1)`
  - `preview`: 依赖 `check-hugo`;先 `git submodule update --init --recursive`,再 `cd docs && hugo server -D --navigateToChanged --gc`
  - `build`: 依赖 `check-hugo`;`cd docs && hugo --minify --gc`
  - `clean`:`rm -rf docs/public docs/resources/_gen`
  - 顶部 `.PHONY: preview build clean check-hugo`
- [x] 5.2 本机直接执行 `cd docs && hugo --minify --gc`(make 命令未安装),确认 `docs/public/index.html` 存在且包含 14 章 + 附录共 15 个章节中文标题
- [x] 5.3 Hugo 构建验证通过,左侧目录树在 index.html 中正确渲染 14 章 + 附录共 15 个章节中文标题,无 404、无主题报错
- [x] 5.4 提交 commit:`chore: add Makefile entry for local preview & build`

## 6. CI Workflow 文件

- [x] 6.1 创建 `.github/workflows/hugo.yml`,框架:
  - `name: Build & Deploy Hugo to GitHub Pages`
  - `on`: 仅 `push.branches: [main]` + `workflow_dispatch`
  - 顶层 `permissions`:`contents: read`、`pages: write`、`id-token: write`(不多不少)
  - 顶层 `concurrency`:`group: pages`、`cancel-in-progress: false`(避免并发 deploy)
  - 两个 job:`build`(runs-on: ubuntu-latest)与 `deploy`(needs: build,environment: github-pages)
- [x] 6.2 `build` job 步骤顺序:
  - `actions/checkout@v4`,参数 `submodules: recursive`、`fetch-depth: 0`
  - `peaceiris/actions-hugo@v3`(pin tag,本次实施时记录具体 v 号),参数 `hugo-version: '0.160.0'`、`extended: true`
  - `actions/configure-pages@v5`
  - `run: hugo --minify --gc`,`working-directory: docs`
  - `actions/upload-pages-artifact@v3`,`path: docs/public`
- [x] 6.3 `deploy` job 步骤:`actions/deploy-pages@v4`,id `deployment`,把 `${{ steps.deployment.outputs.page_url }}` 注入 environment URL
- [x] 6.4 yaml lint 通过(本机 `python -c 'import yaml; yaml.safe_load(open(".github/workflows/hugo.yml"))'` 不抛异常)
- [x] 6.5 提交 commit:`ci: add GitHub Actions workflow to build & deploy Hugo to Pages`

## 7. 用户介入:GitHub 仓库与 Pages 启用 ⚠ 暂停

> **以下三步需要用户登录 GitHub 完成,Claude 在第 7.1 之前会停下,等待用户确认或代为操作完成,再继续 7.5。**

- [x] 7.1 [需用户操作] 在浏览器登录 GitHub,在 `huyiyu` 用户/组织下创建 public 仓库,仓库名严格为 `harnness.github.io`(用户/组织页约定)
- [x] 7.2 [需用户操作] 仓库创建后,在 Settings → Pages 中将 Source 设置为 "GitHub Actions"(不要选择 "Deploy from a branch")
- [x] 7.3 [需用户操作或本地操作,需确认远端 URL] 本地 `git remote add origin git@github.com:huyiyu/harness.git`(或对应 https URL)
- [x] 7.4 [需用户操作] `git push -u origin main`(若用户偏好 SSH,需先确认 ssh key 已配置;若用 https,需配置 GitHub PAT 或 git credential)
- [x] 7.5 等待 GitHub Actions 自动触发首次 workflow,在 Actions 页面观察 build + deploy 两个 job 状态

## 8. 首次部署验证

- [x] 8.1 等待 workflow 状态变为 success(预期 ≤ 5 分钟);若失败,根据日志定位问题(常见:submodule 未 checkout、Hugo 版本不一致、Pages source 未设为 Actions)
- [x] 8.2 浏览器访问 `https://huyiyu.github.io/harness/`,验证:
  - HTTP 200,中文界面 ✓
  - 左侧目录树显示 14 章 + 附录共 15 个章节中文标题 ✓
  - CSS/JS 资源从 `https://huyiyu.github.io/harness/` 加载,无 404 ✓
- [x] 8.3 在仓库 README 中记录首次部署的 commit SHA 与 workflow 运行链接,便于回溯
- [ ] 8.4 (可选)在本地与 CI 之间做一次差异比对:`make build` 后 `diff -r docs/public/ <(actions 产物)`,确保本地与线上产物一致

## 9. 收尾

- [x] 9.1 在 `openspec/changes/init-hugo-harness-plan/` 目录下确认所有 artifact 完整(proposal、design、specs/* × 3、tasks)
- [x] 9.2 运行 `openspec status --change init-hugo-harness-plan --json`,确认 `isComplete = true` 或所有 `applyRequires` 项均 done
- [ ] 9.3 (待用户决定)是否调用 `/opsx:archive` 归档本 change;归档动作不在本次 change 实施范围内,由用户在交付完成后单独决定
