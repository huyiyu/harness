## ADDED Requirements

### Requirement: 7.2 建立 Planner 变体体系

`07-agent-roles` 章节的 7.2 小节 SHALL 建立 Planner 的多种专业化变体，每种变体聚焦"输入类型"差异，展示从通用型到专业型的能力光谱。

#### Scenario: 变体体系完整
- **WHEN** 阅读 7.2 小节
- **THEN** 存在至少 3 个 Planner 变体，每个包含：专长领域、适用场景、与通用型的差异

#### Scenario: 输入类型差异明确
- **WHEN** 阅读任意两个 Planner 变体的对比
- **THEN** 核心差异体现在"接收什么输入、产出什么 Design"上

#### Scenario: 变体不变成清单
- **WHEN** 阅读每个 Planner 变体
- **THEN** 每个变体都回答了"为什么需要这个专业化"——通用型 Planner 在这个场景下会失败的具体原因
