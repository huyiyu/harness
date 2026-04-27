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
