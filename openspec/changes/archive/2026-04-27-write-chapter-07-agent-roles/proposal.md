## Why

第六章建立了 Harness 的全生命周期闭环框架——四阶段管道、上下文交接接力、监控回流驱动。但这个框架只是一个"容器"，容器里装什么角色、这些角色有什么能力边界、它们之间如何协作，需要更细粒度的设计。前六章把 Planner/Generator/Evaluator 当作单一角色来论述，但真实工程中，复杂的系统需要**专业化的角色分工**——不同的场景需要不同专长的 Planner、不同技术栈需要不同技能的 Generator、不同质量维度需要不同验收标准的 Evaluator。第七章需要填补这个空白，完成上篇理论指导思想的收官。

## What Changes

- 新增第七章"Agent 角色体系与分工模型"（`docs/content/docs/07-agent-roles/_index.md`）
- 7.1 为什么需要多种角色——从"三个通用角色"到"多维度专业化分工"的认知升级
- 7.2 Planner 的变体——需求分析型、架构规划型、安全规划型等专业化 Planner
- 7.3 Generator 的变体——前端、后端、数据库、DevOps 等专业化 Generator
- 7.4 Evaluator 的变体——功能、性能、安全、业务价值等专业化 Evaluator
- 7.5 角色协作协议——多角色之间如何协商、如何交接、如何避免冲突
- 7.6 本章小结——上篇理论收官，引出下篇工程实践
- 更新 Hugo 章节索引（如需要）

## Capabilities

### New Capabilities
- `agent-role-specialization`: 7.1 角色专业化必要性的内容规格——为什么单一角色无法满足复杂系统
- `planner-variants`: 7.2 Planner 变体体系的内容规格——多种专业化 Planner 的能力边界
- `generator-variants`: 7.3 Generator 变体体系的内容规格——多种专业化 Generator 的技术分工
- `evaluator-variants`: 7.4 Evaluator 变体体系的内容规格——多种专业化 Evaluator 的验收维度
- `role-collaboration-protocol`: 7.5 角色协作协议的内容规格——多角色协商、交接、冲突避免机制

### Modified Capabilities
- 无现有 spec 需要修改。

## Impact

- 新增一个完整章节到 docs 站点
- 与第六章（闭环框架）和第八章（总体架构）在逻辑上衔接
- 不涉及代码变更，纯内容创作
