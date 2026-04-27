## ADDED Requirements

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
