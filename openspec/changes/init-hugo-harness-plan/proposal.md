## Why

团队需要一份结构化、可迭代、可评审的"Harness 工程落地方案"开发计划。这里的 "Harness" 指 Anthropic 在 [Harness Design for Long-Running Application Development](https://www.anthropic.com/engineering/harness-design-long-running-apps) 中定义的**围绕长时运行 LLM 智能体的编排脚手架**(planner / generator / evaluator 多智能体、sprint 契约、上下文重置/压缩、Playwright MCP 评估等),而不是 harness.io 那个 CI/CD 平台。但与原论文逐节对应不同,本方案以 Harness 思想作为**指导思想**,用于解决软件研发全生命周期(需求→架构→研发→测试→部署→运维→归档复盘)的自动化流程设计,搭建"需求→实施→评估→再实施"的持续闭环。方案融合 DevOps 持续交付理论与 SRE 可靠性工程理论,采用"理论指导思想(50%) + 落地实践路径(50%)"的双篇结构,理论篇提炼 Harness 核心思想与工程体系融合,实践篇覆盖全生命周期各阶段的 AI 主导执行与人类验收推进机制。

当前仓库下 `docs/` 为空,既没有写作环境也没有章节骨架,作者每次写作都要重新决定目录、格式、预览方式;评审也只能在 markdown diff 上做,缺少在线可读版本。先把"用 Hugo 写计划 + 通过 GitHub Pages 在线发布"这件事的环境、目录骨架、自动构建链路一次立起来,后续写正文只需往既定章节里填内容,push 即触发更新,Review 也能按章节滚动推进。

## What Changes

- 在仓库 `docs/` 下初始化一个 Hugo 站点,作为 Harness 工程方案的写作与预览载体。
- 集成 Hugo Book 主题(以 git submodule 方式接入),作为左侧目录树 + 长文阅读的默认主题。
- 站点默认中文(`languageCode: zh-cn`),保留后续扩展多语言的可能性,但本次不开启。
- 写入最小可运行的 `hugo.toml`:站点元信息、主题、Book 主题关键开关(目录展开、搜索、编辑链接占位);`baseURL` 设置为最终发布地址 `https://harnness.github.io/`,本地预览由 `hugo server` 自动覆盖。
- 在 `content/` 下落地"Harness 工程落地方案"的章节大纲(仅章节 `_index.md` 占位 + front matter,不写正文),采用"上篇理论(7 章)+ 下篇实践(7 章)+ 附录"的双篇结构,章节顺序与编号固定,便于后续 change 增量填充。
- 新增 `Makefile` 封装本地命令(`make preview` / `make build` / `make clean`),统一团队入口。
- 新增 `.github/workflows/hugo.yml`,在每次 push 到默认分支时自动 `hugo --minify`,并通过官方 `actions/deploy-pages` 发布到 GitHub Pages。
- 在 GitHub 上创建用户/组织页仓库 `harnness/harnness.github.io`(或在 `harnness` 用户下创建同名仓库),并将本地仓库设为远端推送目标。该步骤需要用户登录 GitHub 完成,Claude 不会代为创建。
- 配置 GitHub Pages 的 source 为 GitHub Actions(而非 gh-pages 分支),与 `actions/deploy-pages` 工作流匹配。

## Capabilities

### New Capabilities

- `hugo-docs-site`: 用 Hugo + Book 主题驱动的中文工程文档站点骨架,涵盖站点配置、主题集成、archetypes、目录约定、本地预览入口。不包含正文内容,但为正文与部署预留稳定锚点。
- `harness-plan-outline`: "Harness 工程落地方案"开发计划的章节骨架(章节命名、顺序、front matter 约定、占位 `_index.md`),共 14 章 + 附录。上篇 7 章为理论指导思想(Harness 核心思想、DevOps 理论、SRE 理论、人机协作范式、闭环框架、Agent 角色体系),下篇 7 章为落地实践路径(总体架构、需求阶段、架构设计、研发、测试、部署运维、归档复盘),每章采用"AI 主导执行→产出待验收物→人验收→触发下一阶段"的统一结构,为后续 change 逐章填充提供稳定锚点。
- `pages-deploy`: 通过 GitHub Actions 自动构建 Hugo 站点并发布到 GitHub Pages 的部署链路,涵盖 workflow 文件、Pages 配置、`baseURL` 与 submodule checkout 等约定。

### Modified Capabilities

<!-- 当前 openspec/specs/ 为空,无既有 capability 被修改 -->

## Impact

- 新增目录:`docs/`(Hugo 站点根目录),包含 `hugo.toml`、`content/`、`themes/hugo-book/`(submodule)、`archetypes/`、`static/`、`layouts/` 占位。
- 新增 `.github/workflows/hugo.yml`,使用 `actions/checkout`(含 `submodules: recursive`)+ `peaceiris/actions-hugo` 或官方 setup-hugo + `actions/configure-pages` + `actions/upload-pages-artifact` + `actions/deploy-pages` 链路。
- 新增 `.gitmodules`(包含 hugo-book 主题 submodule)。
- 新增根级 `Makefile`,统一封装本地命令。
- 仓库需关联 GitHub 远端 `harnness/harnness.github.io` 并推送默认分支;此动作 **由用户在浏览器登录 GitHub 完成**,实施过程中 Claude 会停在该步骤等待。
- 不影响现有 `openspec/` 工作流;后续每章写作各自走独立 change。
- 依赖:
  - 本机已安装 Hugo extended ≥ 0.128(已确认 v0.160.0)。
  - Git ≥ 2.30(submodule 支持)。
  - GitHub 账户/组织 `harnness` 存在,且具备创建 public 仓库与启用 Pages 的权限。
- 风险:
  - 用户/组织页 (`<name>.github.io`) 全站根路径即站点根,`baseURL` 必须为 `https://harnness.github.io/`(末尾斜杠不可省),否则资源相对路径会错乱。
  - submodule 在 GitHub Actions 中默认不会 checkout,workflow 必须显式开启 `submodules: recursive`,否则构建会因主题缺失而失败。
  - GitHub Pages 启用过程涉及一次手动设置(Settings → Pages → Source = GitHub Actions),不能完全脚本化,需要在 tasks 中显式记录这一步。
