## ADDED Requirements

### Requirement: 第一章正文采用叙事风格且面向决策者

`01-background` 章节的正文 SHALL 采用叙事化写作风格，面向管理层与决策者，避免技术术语和实现细节。

#### Scenario: 正文不含技术术语
- **WHEN** 阅读 `01-background/_index.md` 的正文部分（front matter 之后）
- **THEN** 不出现"上下文窗口""token""压缩""MCP""Playwright""评估器调优"等工程术语

#### Scenario: 包含业务视角的痛点描述
- **WHEN** 阅读第一章正文
- **THEN** 内容将 AI 长时开发问题翻译为决策者可感知的业务影响（交付周期、质量风险、规模化瓶颈）

### Requirement: 第一章只提问题不讲 Harness 原理

第一章正文 SHALL 聚焦问题呈现，不在正文中展开解释 Harness 的核心组件（planner/generator/evaluator）或工作机制（sprint 契约、上下文交接）。Harness 的定义和原理留给第二章。

#### Scenario: 正文不解释 Harness 工作原理
- **WHEN** 阅读第一章正文
- **THEN** 不出现 planner、generator、evaluator 的职能描述，不出现 sprint 契约或上下文交接机制的技术解释

#### Scenario: 章节末尾轻量引出 Harness 概念
- **WHEN** 阅读到第一章末尾
- **THEN** 出现 Harness 的命名提及（如"业界开始用一套称为 Harness 的方法应对这些问题"），但不做技术展开

### Requirement: 引用 Claude Code / Anthropic 实践案例

第一章正文 SHALL 引用 Anthropic 工程团队或 Claude Code 在长时开发任务中的真实观察，作为痛点故事的事实基础。

#### Scenario: 包含一手实践案例
- **WHEN** 阅读第一章正文
- **THEN** 至少包含一个来源于 Anthropic 官方博客、论文或 Claude Code 实践的长时开发案例片段

#### Scenario: 案例服务于工程化落地难题的铺垫
- **WHEN** 阅读案例相关内容
- **THEN** 案例叙事指向"AI 辅助开发从原型到工程化落地"的困境，而非单纯展示 AI 能力
