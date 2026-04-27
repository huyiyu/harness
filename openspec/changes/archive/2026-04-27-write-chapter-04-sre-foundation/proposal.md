## Why

前三章已建立 Harness 的背景铺垫、核心思想和 DevOps 基础。但 Harness 框架在生产环境中运行时，可靠性是不可回避的问题——AI 可以分钟级迭代代码，但如果产出的代码在上线后引发故障，迭代速度再快也没有意义。本章需要将 SRE（Site Reliability Engineering，站点可靠性工程）的理论与 Harness 框架对接，回答"AI 自治的编码流程如何满足生产级可靠性要求"这个问题，为决策者提供可靠性视角的评估依据。

## What Changes

- 撰写第四章"SRE 与可靠性工程理论"正文
- 内容包括：
  - SRE 核心概念（SLO/SLI/SLA、错误预算）的通俗化解读
  - 错误预算与 Harness 评估轮次/成本权衡的关联分析
  - 监控（Monitoring）与可观测性（Observability）在 Harness 反馈环中的角色
  - 混沌工程（Chaos Engineering）理念对 Harness 评估标准设计的启发
  - "程序员作为最终责任人"的 SRE 实践定位
- 遵循 chapter-content-standard 的内容质量标准，包含"内容说明"章节
- 使用 Mermaid 图表辅助说明概念

## Capabilities

### New Capabilities
<!-- 本章为纯内容写作，沿用已有 chapter-content-standard，无需新增 spec -->
（无新增能力——本章为书籍内容创作，遵循已有 chapter-content-standard 规范）

### Modified Capabilities
（无——本章不涉及已有 spec 的需求变更）

## Impact

- 新增文件：`docs/content/docs/04-sre-foundation/_index.md`
- 新增目录：`docs/content/docs/04-sre-foundation/`
- 沿用 Hugo Docs 站点已有配置，无需修改站点结构
