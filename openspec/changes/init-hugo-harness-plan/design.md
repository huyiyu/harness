## Context

仓库根目录 `/home/huyiyu/Documents/harnness` 当前为空白工程,只有 `.claude/`、`docs/`(空)、`openspec/`(空)。本次 change 要把 `docs/` 改造成一个可写、可预览的 Hugo 站点,并通过 GitHub Actions 自动发布到 `https://huyiyu.github.io/`。环境已确认:Hugo extended `v0.160.0`、Git 可用、Node v22 可用。GitHub 上目标仓库 `huyiyu/harnness.github.io` 尚未创建,需用户登录后手工创建。本仓库尚未 `git init`,也未关联远端,这些都是本次 change 的实施前置。

**写作主题与定位**:本方案的"Harness"采用 Anthropic 文章 [Harness Design for Long-Running Application Development](https://www.anthropic.com/engineering/harness-design-long-running-apps) 中的语义——围绕长时运行 LLM 智能体的编排脚手架(planner / generator / evaluator + sprint 契约 + 上下文管理 + 工具集成),不是 harness.io 这种 CI/CD 平台。但与原论文逐节对应不同,本方案以 Harness 思想作为**指导思想**,用于解决软件研发全生命周期(需求→架构→研发→测试→部署→运维→归档复盘)的自动化流程设计。方案采用"理论指导思想(50%) + 落地实践路径(50%)"的双篇结构,理论篇融合 Harness 核心思想、DevOps 持续交付理论与 SRE 可靠性工程理论;实践篇覆盖全生命周期各阶段的 AI 主导执行与人类验收推进机制。

## Goals / Non-Goals

**Goals:**
- 在 `docs/` 下产出一个 30 秒内可 `hugo server` 起来、并且左侧目录能正确展示章节大纲的最小站点。
- 目录与命名规则一旦确定,后续每一章都能"开一个 change → 往固定路径写正文 → push → CI 自动发布",不需要再回过头改站点结构或 workflow。
- 每次推送默认分支后 ≤ 3 分钟内,`https://harnness.github.io/` 反映最新内容,且不依赖任何本地构建。
- 主题以可控、可升级、可离线还原的方式接入,不与某个外部 registry 强绑定。
- 默认中文呈现,文档内不出现英文模板残留。

**Non-Goals:**
- 不写任何 Harness 工程方案的正文(连 1 段都不写),只放章节占位 `_index.md`。
- 不接入除 GitHub Pages 之外的部署目标(Netlify / Vercel / 自建 CDN)。
- 不开启 i18n 多语言切换;`languageCode` 仅置为 `zh-cn`。
- 不二次开发主题(不写自定义 layouts、shortcodes、partials)。
- 不引入 Algolia 等外部搜索;搜索能力随 Hugo Book 主题自带功能开启或关闭。
- 不锁定章节正文长度、写作风格、Review 流程,这些放到逐章 change 里再定。
- 不在本次 change 中处理自定义域名(CNAME)、HTTPS 强制、访问统计等增强项。

## Decisions

### 决策 1:Hugo Book 主题以 git submodule 方式接入,不使用 Hugo Module

- **选择**:`git submodule add https://github.com/alex-shpak/hugo-book.git docs/themes/hugo-book`,在 `docs/hugo.toml` 中 `theme = "hugo-book"`,并 pin 到具体 tag。
- **理由**:
  - 团队当前没有 Go 工具链强制要求,submodule 比 Hugo Module 心智负担小。
  - submodule pin 到具体 commit,主题升级是显式动作。
  - 离线 / 内网环境下 `git clone --recursive` 即可完整还原。
  - GitHub Actions 中只需在 `actions/checkout` 上加一个 `submodules: recursive` 即可,改动面小。
- **替代方案**:Hugo Modules 升级体验更好,但要求 Go 工具链与稳定的 module proxy,为一个写作仓库引入这层依赖不划算。

### 决策 2:`hugo.toml` 而非 `hugo.yaml` / `config/_default/`

- **选择**:单文件 `hugo.toml` 放在 `docs/` 根。
- **理由**:Hugo 官方与 Book 主题示例都用 TOML;本次仅有一份配置(production 与本地预览靠 baseURL 在 `hugo server` 时被自动覆盖),不需要分层。
- **替代方案**:`config/_default/` 多环境分层,等真要区分 prod/staging 时再升级。

### 决策 3:`baseURL` 直接写最终发布地址,本地预览靠 `hugo server` 覆盖

- **选择**:`baseURL = "https://huyiyu.github.io/"`(末尾斜杠保留)。
- **理由**:
  - 用户/组织页 `<name>.github.io` 部署在域名根路径,无需 path prefix,`baseURL` 与本地预览之间差异最小。
  - `hugo server` 启动时会将 `baseURL` 自动改写为 `http://localhost:1313/`,本地体验不受影响。
  - 避免在 workflow 里再次传 `--baseURL`,减少配置漂移。
- **替代方案**:`baseURL = "/"` 让 Hugo 输出相对路径;但 RSS、Open Graph 元信息会丢失绝对地址,不可取。

### 决策 4:章节用"两位数前缀目录"组织,而非 `weight` 字段

- **选择**:`docs/content/01-background/_index.md`、`docs/content/02-failure-modes/_index.md`...
- **理由**:目录名本身排序 = 章节顺序,新增章节只需插入新前缀;Book 主题在没有显式 `weight` 时也会回退到目录名字典序,行为一致。
- **替代方案**:全部走 `weight`,但散落在各文件 front matter 里,新增/重排易出错。

### 决策 5:章节大纲采用"上篇理论 + 下篇实践"双篇结构,共 14 章 + 附录

- **选择**:本次 change 一次性建 14 个章节目录与 `_index.md`(上篇 7 章 + 下篇 7 章),外加 `99-appendix`,后续 change 只填正文。章节切分不再与 Anthropic harness 论文逐节对应,而是以论文思想为**指导思想**,按"理论 50% + 实践 50%"的原则组织:上篇聚焦 Harness 核心思想、DevOps/SRE 理论体系融合、人机协作范式与闭环框架设计;下篇聚焦全生命周期(需求→架构→研发→测试→部署→运维→归档)各阶段的 AI 主导执行与人类验收推进机制。
- **章节清单**(顺序即编号):
  - **上篇·理论指导思想**:
    1. `01-background` — 背景与 Harness 定义(问题域、Harness 论文定位、与传统 CI/CD 的区别)
    2. `02-harness-core` — Harness 核心思想：评估驱动的自主 Agent 闭环(任务分解、执行、评估、迭代的闭环逻辑)
    3. `03-devops-foundation` — DevOps 与持续交付理论(CI/CD 基础设施、三步工作法、流动-反馈-持续学习)
    4. `04-sre-foundation` — SRE 与可靠性工程理论(错误预算、SLI/SLO/SLA、自动化运维、故障响应)
    5. `05-collaboration-paradigm` — 人机协作范式：验收驱动模型(执行权与验收权分离、HITL/HOTL/HOOTL、渐进式放权)
    6. `06-closed-loop-framework` — 全生命周期闭环框架设计(PDCA + OODA + Agent 迭代增强的统一模型)
    7. `07-agent-roles` — Agent 角色体系与分工模型(需求 Agent、架构 Agent、编码 Agent、测试 Agent、部署 Agent、运维 Agent、复盘 Agent 的职责定义与协作接口)
  - **下篇·落地实践路径**:
    8. `08-overall-architecture` — 总体架构：AI 执行引擎与人类验收层(平台四层架构、关键路径状态机、人机交互层设计)
    9. `09-requirement-phase` — 需求阶段：AI 主导需求工程,人做准入验收(需求收集→结构化分析→待验收物→人工审批→触发下一阶段)
    10. `10-design-phase` — 架构设计阶段：AI 主导技术方案,人做架构确认(方案生成→验证→待验收物→人工审批→触发编码 Agent)
    11. `11-development-phase` — 研发阶段：AI 主导编码实现,人做代码验收(代码生成→Code Review→待验收物→人工审批→触发测试 Agent)
    12. `12-testing-phase` — 测试阶段：AI 主导质量验证,人做发布审批(测试生成→执行→报告→待验收物→人工审批→触发部署 Agent)
    13. `13-deployment-ops-phase` — 部署与运维阶段：AI 主导系统保障,人做上线确认与重大事件决策(发布执行→监控→异常响应→待验收物→人工确认)
    14. `14-review-evolution-phase` — 归档复盘与 AI 持续进化(数据聚合→复盘报告→改进确认→验收数据驱动 Agent 迭代)
  - **附录**:
    15. `99-appendix` — 附录(术语表、参考实现参数、度量指标汇总、论文引用索引)
- **理由**:
  - 双篇结构确保理论深度与实践落地并重,避免单纯复述论文或变成无理论支撑的操作手册。
  - 上篇从 Harness 论文出发,向外融合 DevOps/SRE 两大工程体系,向内提炼人机协作范式和闭环框架,为下篇实践提供统一的理论基础。
  - 下篇按软件全生命周期顺序展开,每章统一采用"AI 执行(主导)→产出待验收物→人验收(关键路径门禁)→触发下一阶段"的四段式结构,便于读者建立一致的认知模型。
  - `07-agent-roles` 作为上篇与下篇的衔接章,从理论角色定义过渡到实践中的 Agent  instantiation,承上启下。
  - 编号 `01-07` 为上篇、`08-14` 为下篇,`99-appendix` 固定为附录。若后续需插入独立大节(如"案例研究"、"工具链详解"),中段 `15-98` 预留充足空间。

### 决策 6:GitHub Actions workflow 走官方 Pages 链路

- **选择**:`.github/workflows/hugo.yml` 使用以下官方/主流 actions 组合:
  - `actions/checkout@v4`,带 `submodules: recursive` 与 `fetch-depth: 0`(后者是为 `.GitInfo` lastmod)。
  - `peaceiris/actions-hugo@v3`(社区主流,显式 pin 到一个 tag)安装 Hugo extended,版本与本地保持一致(0.160.0)。
  - `actions/configure-pages@v5` 注入 Pages 元信息。
  - 在 `docs/` 下执行 `hugo --minify --gc`。
  - `actions/upload-pages-artifact@v3` 上传 `docs/public/`。
  - `actions/deploy-pages@v4` 发布。
- **理由**:
  - 官方 deploy-pages 链路是 GitHub 当前推荐方案,放弃旧的 `gh-pages` 分支可避免一份产物在仓库历史里反复打 commit。
  - `peaceiris/actions-hugo` 比手工下载二进制更稳,且能精确指定 extended 版本。
- **替代方案**:
  - 自己 `wget` Hugo 二进制并解压:维护成本更高且签名校验麻烦。
  - 走 `gh-pages` 分支:可读性差,且需要单独的部署 token 配置;不可取。

### 决策 7:workflow 触发条件限制为 `main` 与 `workflow_dispatch`

- **选择**:`on: { push: { branches: [main] }, workflow_dispatch: {} }`,不监听 PR。
- **理由**:
  - 用户/组织页只有一份生产部署,PR 阶段不需要构建发布;监听 PR 反而会重复消耗 Pages 部署额度。
  - 保留 `workflow_dispatch` 便于手工重跑(主题更新后回填)。
- **替代方案**:加 PR preview 部署(Vercel / Netlify);不在本次范围。

### 决策 8:本地预览统一入口走 `Makefile`

- **选择**:仓库根新增 `Makefile`,提供 `make preview`、`make build`、`make clean` 三个目标,内部封装 `hugo` 命令并固定参数。
- **理由**:统一入口避免"漏 -D 看不到草稿"等支持成本,且 `make build` 与 CI 行为对齐(都走 `hugo --minify --gc`),便于本地复现 CI 失败。

### 决策 9:Front matter 模板由 archetypes 兜底

- **选择**:在 `docs/archetypes/default.md` 写入统一 front matter:`title`、`weight`(空占位)、`bookCollapseSection`(章节根 true)、`draft: true`。新建 `_index.md` 走 `hugo new content <path>` 自动套用。
- **理由**:Book 主题对 `bookCollapseSection`、`bookHidden`、`bookFlatSection` 等字段有特殊语义,统一模板防止后来者忘记设置导致目录树异常。

### 决策 10:Pages source 设置为"GitHub Actions"而非分支

- **选择**:在仓库 Settings → Pages 中将 Source 选为 "GitHub Actions"。
- **理由**:与决策 6 中的 `actions/deploy-pages` 链路匹配;不留 `gh-pages` 分支占用历史。
- **影响**:这步操作不能脚本化,必须用户在浏览器登录 GitHub 完成,tasks.md 中作为人工步骤显式列出。

## Risks / Trade-offs

- **[风险] submodule 在 CI 中默认不被 checkout,导致主题缺失构建失败**
  → 缓解:workflow 中显式 `submodules: recursive`,本次 change 的 tasks.md 中专门有一项验证 CI 首次构建成功。
- **[风险] Hugo extended 与非 extended 版本对 SCSS 处理行为不同**
  → 缓解:`peaceiris/actions-hugo` 的 `extended: true` 显式开启;`Makefile preview` 在本地也校验 `hugo version | grep extended`。
- **[风险] `harnness.github.io` 仓库名与 GitHub 用户/组织名严格绑定**
  → 缓解:本次 change 的实施前置是用户在 GitHub 上确认 `huyiyu` 这个用户/组织存在并拥有它;若不存在,需用户先注册或选用其他名字,Claude 会停在该前置上等待。
- **[风险] Pages 启用过程涉及手动 UI 操作,容易遗忘**
  → 缓解:tasks.md 中独立列出"手工开启 Pages → Source: GitHub Actions"步骤,且首次 push 后 CI 失败时,在 design 中预留 troubleshooting 注释。
- **[风险] `99-appendix` 这种数字编号风格可能让外部读者误解为"99 章"**
  → 缓解:在 `99-appendix/_index.md` 的 front matter 设置 `title: 附录`,目录树里显示中文标题,数字仅作为路径排序;上篇(01-07)与下篇(08-14)在首页或 `_index.md` 中用文字明确标注分篇。
- **[风险] 主题或 Hugo 版本升级触发的 breaking change 会让 CI 突然失败**
  → 缓解:submodule 与 actions-hugo 的版本都 pin 到具体 tag/commit,升级走独立 change。
- **[权衡] 14 章固定骨架在写作过程中可能发现需要拆/合]**
  → 缓解:接受 1-2 次后续骨架调整 change 的成本;比每次写作都讨论目录结构便宜。上篇与下篇的边界(07→08)和具体章节粒度(如 13-deployment-ops-phase 是否拆分为部署、运维两章)可在首批正文 change 中根据实际内容量再微调。
- **[权衡] 暂不接入 PR preview,意味着评审需在 main 合并后才看到效果**
  → 缓解:本地 `make preview` 已能完整复现产物;真正需要 PR preview 时再开独立 change(可挂 Cloudflare Pages 或 Netlify Drafts)。

## Migration Plan

无既存系统需要迁移,但实施过程涉及**外部副作用**,顺序如下,中间需用户操作的步骤会显式停顿:

1. 本地完成 `git init`、初始化 Hugo 站点、提交所有骨架文件(本机操作,Claude 可执行)。
2. 本地建好 submodule 与 workflow 文件,本地 `make build` 预演 CI 行为通过(本机操作,Claude 可执行)。
3. **[需用户操作]** 在 GitHub 上创建 `huyiyu/harnness.github.io` 仓库(或在用户 `huyiyu` 名下创建同名仓库)。
4. **[需用户操作或授权]** 将本地仓库 `git remote add origin git@github.com:huyiyu/harnness.github.io.git` 并 `git push -u origin main`。
5. **[需用户操作]** 在仓库 Settings → Pages 中将 Source 设置为 "GitHub Actions"。
6. 触发首次 workflow 跑通,访问 `https://huyiyu.github.io/` 验收(本机/Claude 可监控,但失败时由用户决定排错方向)。

回滚:本次 change 全部回滚等价于删除 `docs/`、`.github/workflows/hugo.yml`、`.gitmodules`、`Makefile`,以及在 GitHub 上停用 Pages 或删除仓库。

## Open Questions

- 仓库可见性是 public 还是 private?private 仓库的 Pages 需要 GitHub Pro/Team 套餐。**暂定**:public,与开源工程文档惯例一致;若用户希望 private,需在 tasks 实施前提示并切换。
- 是否需要在 `static/` 下预先放 favicon / logo 占位?**暂定**:不放,等团队提供品牌素材。
- 是否启用 Hugo Book 自带的全文搜索(`BookSearch = true`)?**暂定**:启用;若后续发现索引体积或加载性能问题再关闭。
- 是否要在首次部署成功后,把 `harnness.github.io` 链接写回根目录 `README.md`?**暂定**:本次 change 不创建根 README,放到正文写作阶段决定。
- 主题 / Hugo / actions 的具体 pin 版本号在 tasks.md 实施时锁定;design 阶段不锁,以避免 design 重写。
