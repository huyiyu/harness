## Why

前四章已建立 Harness 的技术框架（三角色、验收驱动、闭环），但尚未从"人的视角"回答关键问题：当 AI 接管执行层后，各角色的人类如何与 AI 协作？人的核心价值从"动手"转向什么？第五章需要填补这一空白，建立"验收驱动"的人机协作范式，使全书从技术架构层上升到组织与人才层。

## What Changes

- 新增第五章"人机协作范式：验收驱动模型"（`docs/content/docs/05-collaboration-paradigm/_index.md`）
- 5.1 范式转移：从"人执行"到"人判断"——论证 AI 时代人类工作本质的变化
- 5.2 各角色的 AI 协同模式——分角色阐述产品经理、程序员、架构师、项目经理、测试工程师、实施/运维与 AI 的分工边界
- 5.3 能力门槛重构：数控机床与八级钳工——用类比说明"执行技能贬值、判断技能升值"的人才趋势
- 5.4 验收驱动机制——将 Harness 的 Evaluator 思想映射到人类工作中，建立人的"验收权"核心地位
- 5.5 本章小结
- 更新 hugo.toml 或章节索引（如需要）

## Capabilities

### New Capabilities
- `collaboration-paradigm-shift`: 5.1 范式转移的内容规格——从"人执行"到"人判断"的核心论点和论证结构
- `role-ai-collaboration`: 5.2 各角色的 AI 协同模式——六个角色的分工边界、AI 接管范围、人类保留的验收/判断能力
- `skill-barrier-reconstruction`: 5.3 能力门槛重构——数控机床与八级钳工类比、执行技能贬值与判断技能升值的论证
- `human-acceptance-mechanism`: 5.4 验收驱动机制——人类在 Harness 闭环中的验收角色、验收标准设计、与 AI Evaluator 的协作关系

### Modified Capabilities
- 无现有 spec 需要修改。

## Impact

- 新增一个完整章节到 docs 站点
- 与前后章（第四章 SRE 基础、第六章闭环框架）在逻辑上衔接
- 不涉及代码变更，纯内容创作
