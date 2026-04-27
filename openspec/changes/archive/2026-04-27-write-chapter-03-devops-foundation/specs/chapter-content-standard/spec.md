## ADDED Requirements

### Requirement: 第三章正文精炼聚焦 Harness 与 DevOps 交汇点

`03-devops-foundation` 章节的正文 SHALL 采用精炼版篇幅（1500-2000 中文字符），聚焦 Harness 与 DevOps 的交汇点，不做 DevOps 理论的全面科普。

#### Scenario: 篇幅精炼
- **WHEN** 阅读 `03-devops-foundation/_index.md` 的正文
- **THEN** 正文中文字符数在 1500-2000 范围内

#### Scenario: 核心论点突出
- **WHEN** 阅读第三章正文
- **THEN** 80% 以上篇幅围绕"Harness 是 DevOps 的终极形态——反馈环速度"这一核心论点展开

### Requirement: 第三章结构遵循 ACB 占比 8:1:1

第三章正文 SHALL 遵循 A:C:B = 8:1:1 的结构比例：A（Harness 是 DevOps 终极形态）、C（DevOps 是 Harness 土壤）、B（需要重新诠释的实践）。

#### Scenario: A 部分占主导
- **WHEN** 分析第三章正文的段落分布
- **THEN** 关于"反馈环速度"的论述占全文主体，C 和 B 部分各不超过 2 个段落

#### Scenario: C 和 B 部分轻量
- **WHEN** 阅读 C 和 B 部分内容
- **THEN** 每部分控制在 200 中文字符以内，点到为止

### Requirement: 弱化 Evaluator 验收与自动化测试的层次区分

第三章正文 SHALL 将 Evaluator 验收与 DevOps 自动化测试视为融合关系，不强调层次区分。

#### Scenario: 融合表述
- **WHEN** 阅读涉及 Evaluator 验收和自动化测试的段落
- **THEN** 采用"升级""延伸"等融合性表述，不出现"第一层/第二层""AI 验收 vs DevOps 测试"等对立性表述

### Requirement: 第三章末尾包含内容说明段落

第三章正文 SHALL 在末尾包含"**本章内容说明**"段落，统一标注论文原意、作者分析、工程扩展、虚构示例四类内容的来源。

#### Scenario: 内容说明段落完整
- **WHEN** 阅读到第三章末尾
- **THEN** 存在"**本章内容说明**"段落，明确区分并列出：论文原意（源自 Anthropic 论文的内容）、作者分析（基于论文的分析推导）、工程扩展（落地实践补充）、虚构示例（构造的说明性场景）
