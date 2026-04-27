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

### Requirement: 第二章正文准确传达 Harness 核心思想

`02-harness-core` 章节的正文 SHALL 准确介绍 Anthropic Harness 论文的核心设计，包括三角色（Planner/Generator/Evaluator）、验收标准机制、信息交接，并在此基础上补充工程落地的人类角色扩展。

#### Scenario: 三角色定义准确
- **WHEN** 阅读 `02-harness-core/_index.md` 的正文
- **THEN** Planner 被描述为将需求拆解为任务清单的角色，Generator 被描述为按任务逐个实现产出的角色，Evaluator 被描述为独立验收产出质量的角色

#### Scenario: 验收标准定位为 AI-AI 协商机制
- **WHEN** 阅读关于验收标准的段落
- **THEN** 明确描述为 Generator（实现者）与 Evaluator（验收方）在编码前就"完成定义"达成一致的机制，不是人类与 AI 之间的协商

#### Scenario: 区分论文原意与落地扩展
- **WHEN** 阅读关于人类角色的段落
- **THEN** 明确标注"AI 自治三角色 + AI-AI 验收标准"是 Anthropic 论文的原意，"人类验收校准"是工程落地的补充扩展

### Requirement: 第二章采用轻量化术语策略

第二章正文 SHALL 采用"专业名词在前、括号通俗解释在后"的策略，首次出现时同时给出两者，后续可单独使用专业名词。

#### Scenario: 首次出现附带通俗解释
- **WHEN** 阅读第二章正文中 Planner、Generator、Evaluator、验收标准、轮次、信息交接等术语的首次出现
- **THEN** 格式为"专业名词（通俗解释）"，如"Planner（任务拆解）"

#### Scenario: 术语一致性
- **WHEN** 检查全文章节
- **THEN** 同一术语的通俗解释在全文中保持一致，不出现同一概念多个不同解释

### Requirement: 人类角色论述完整涵盖三重必要性

第二章正文 SHALL 完整阐述人类在 Harness 中的三重角色，定位为功能性必要而非心理安慰。

#### Scenario: 需求校准包含双向偏差来源
- **WHEN** 阅读"需求校准"相关内容
- **THEN** 明确指出偏差可能来自：人未表达清楚、AI 理解错误、战略已调整，人类验收的作用是"校准"而非"纠错"

#### Scenario: 及时止损使用正确比喻
- **WHEN** 阅读"及时止损"相关内容
- **THEN** 使用"踩刹车"而非"拆刹车"等错误比喻

#### Scenario: 建立信心定位为组织决策信息
- **WHEN** 阅读"建立可预期信心"相关内容
- **THEN** 描述为标准化验收让管理层能判断项目质量水平，是可操作的决策信息，不是空洞的心理安慰

## MODIFIED Requirements

### Requirement: 第四章正文引入 SRE 与 Harness 的概念桥梁

第四章 `04-sre-foundation` 章节的正文 SHALL 建立 Harness 与 SRE（Site Reliability Engineering，站点可靠性工程）理论之间的概念映射，将 SRE 核心概念作为理解 Harness 可靠性维度的框架。

#### Scenario: SLO 与 Evaluator 验收标准的映射
- **WHEN** 阅读第四章正文
- **THEN** 存在 SLO（Service Level Objective，服务等级目标）与 Evaluator 验收标准的类比论述，说明两者都是"足够好"的定义

#### Scenario: 错误预算与成本权衡的映射
- **WHEN** 阅读第四章正文
- **THEN** 存在"错误预算（Error Budget）"概念与 Harness 评估轮次/token 成本预算的关联分析

#### Scenario: 可观测性与监控反馈的映射
- **WHEN** 阅读第四章正文
- **THEN** 存在"可观测性（Observability）"与 Harness 部署后监控反馈回流的理论提升论述

#### Scenario: 混沌工程与边界测试的映射
- **WHEN** 阅读第四章正文
- **THEN** 存在"混沌工程（Chaos Engineering）"理念对 Evaluator 验收标准设计的启发论述

### Requirement: 第四章采用追问式引入并与第三章形成数据呼应

第四章正文 SHALL 从第三章结尾自然追问引入（"分钟级的反馈环如果产出的是不可靠的代码，速度还有什么意义？"），并主动引用第三章的成本数据（$9 vs $200、5-15 轮迭代、硬阈值）在 SRE 框架下重新解读。

#### Scenario: 追问式引入
- **WHEN** 阅读第四章开头
- **THEN** 以追问方式承接第三章，而非独立开篇

#### Scenario: 数据呼应
- **WHEN** 阅读第四章涉及成本或评估轮次的段落
- **THEN** 明确引用第三章的成本数据，并在 SRE 概念下重新诠释

### Requirement: 第四章使用 Mermaid 图表辅助说明

第四章正文 SHALL 包含至少一个 Mermaid 图表，用于说明错误预算与 Harness 迭代轮次之间的关系。

#### Scenario: 图表存在
- **WHEN** 阅读第四章正文
- **THEN** 存在 `{{< mermaid >}}` 代码块，展示质量评分随迭代轮次的变化（含平台期和硬阈值停止线）

### Requirement: 第四章末尾包含内容说明段落

第四章正文 SHALL 在末尾包含"**本章内容说明**"段落，统一标注论文原意、作者分析、工程扩展、虚构示例四类内容的来源。

#### Scenario: 内容说明段落完整
- **WHEN** 阅读到第四章末尾
- **THEN** 存在"**本章内容说明**"段落，明确区分并列出：论文原意（源自 Anthropic 论文的内容）、作者分析（基于论文的分析推导）、工程扩展（落地实践补充）、虚构示例（构造的说明性场景）
