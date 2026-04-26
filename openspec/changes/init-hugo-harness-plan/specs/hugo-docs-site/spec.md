## ADDED Requirements

### Requirement: Hugo 站点根目录与配置文件

系统 SHALL 在仓库 `docs/` 目录下提供一个完整的 Hugo 站点根,包含一个 `hugo.toml` 配置文件,声明站点元信息、主题以及 Hugo Book 主题所需的关键开关。

#### Scenario: 在 docs 目录直接运行 hugo 命令

- **WHEN** 团队成员在 `docs/` 目录执行 `hugo` 或 `hugo server` 命令
- **THEN** Hugo 能够识别该目录为站点根,加载 `hugo.toml` 并完成首次构建/启动,不报缺失配置或主题相关错误

#### Scenario: hugo.toml 包含中文站点最小集合配置

- **WHEN** 任何人查看 `docs/hugo.toml`
- **THEN** 文件中至少包含 `baseURL = "https://harnness.github.io/"`、`languageCode = "zh-cn"`、`defaultContentLanguage = "zh-cn"`、`title`、`theme = "hugo-book"` 五项

### Requirement: Hugo Book 主题以 submodule 方式集成

系统 SHALL 通过 git submodule 将 Hugo Book 主题接入到 `docs/themes/hugo-book/`,并将 submodule 锁定到一个具体的 tag commit,而不是浮动在 master 分支。

#### Scenario: 仓库初次 clone 后能完整还原主题

- **WHEN** 任何人执行 `git clone --recursive <repo>`(或先 clone 再 `git submodule update --init --recursive`)
- **THEN** `docs/themes/hugo-book/` 目录下存在主题完整文件,包含 `theme.toml`、`layouts/`、`assets/`,且 `git submodule status` 显示锁定到具体 commit 不带 `-` / `+` 前缀

#### Scenario: 主题不在 master 浮动

- **WHEN** 检查仓库 `.gitmodules`
- **THEN** 文件包含 `hugo-book` 项,且 submodule 实际指向的 commit hash 等于 hugo-book 的某个 release tag,而非 master HEAD

### Requirement: 本地预览统一入口

系统 SHALL 在仓库根提供一个 `Makefile`,封装 `make preview`、`make build`、`make clean` 三个目标。

#### Scenario: make preview 启动包含草稿的本地预览

- **WHEN** 团队成员在仓库根执行 `make preview`
- **THEN** 命令首先执行 `git submodule update --init --recursive`,然后调用 `hugo server -D --navigateToChanged --gc`,工作目录为 `docs/`,本地服务监听 `1313` 端口

#### Scenario: make build 与 CI 构建参数一致

- **WHEN** 团队成员执行 `make build`
- **THEN** 命令在 `docs/` 工作目录下执行 `hugo --minify --gc`(与 CI workflow 中的构建步骤参数完全一致),产物写入 `docs/public/`

#### Scenario: 缺少 Hugo extended 时立即失败提示

- **WHEN** 当前机器上 `hugo version` 输出不含 `extended` 关键字
- **AND** 团队成员执行 `make preview` 或 `make build`
- **THEN** 命令在启动 Hugo 之前退出,并打印中文错误信息提示"需要 Hugo extended 版本"

#### Scenario: make clean 清理构建产物

- **WHEN** 团队成员执行 `make clean`
- **THEN** `docs/public/`、`docs/resources/_gen/` 两个目录被删除,`docs/content/` 与配置文件不受影响

### Requirement: 站点可在本地零额外配置打开

系统 SHALL 保证 `make preview` 启动后,默认浏览器访问 `http://localhost:1313/` 即可看到中文界面与左侧章节目录树,不出现"未配置主题"、"找不到内容"等空白页或报错页。

#### Scenario: 首页可见左侧目录树

- **WHEN** `make preview` 已启动且无报错
- **AND** 浏览器访问 `http://localhost:1313/`
- **THEN** 页面左侧出现 Hugo Book 风格的目录树,列出全部 14 章 + 附录共 15 个章节标题(中文),点击任一章节进入对应 `_index.md` 页面

### Requirement: archetypes 提供统一 front matter 模板

系统 SHALL 在 `docs/archetypes/default.md` 中提供统一的 front matter 模板,以便后续通过 `hugo new content <path>` 创建新页面时自动套用。

#### Scenario: 新建页面自动获得 draft true 与 weight 占位

- **WHEN** 团队成员在 `docs/` 下执行 `hugo new content 08-overall-architecture/01-overview.md`
- **THEN** 生成的文件 front matter 至少包含 `title`、`draft = true`、`weight`(空值占位)三个字段,且字段为 TOML 格式
