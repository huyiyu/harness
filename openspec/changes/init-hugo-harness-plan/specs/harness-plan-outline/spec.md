## ADDED Requirements

### Requirement: 章节目录采用两位数前缀命名

系统 SHALL 将"Harness 工程落地方案"的所有章节目录放置于 `docs/content/` 下,目录名遵循 `<两位数字>-<英文 kebab-case 短名>` 的格式,目录顺序即章节顺序。数字前缀 SHALL 使用前导零(如 `01`、`02`、`09`)。

#### Scenario: 章节目录可按字典序得到正确章节顺序

- **WHEN** 在 `docs/content/` 下执行 `ls -1`
- **THEN** 输出按字典序排列时,等于实际章节顺序(`01-background` 在 `02-harness-core` 之前,以此类推)

#### Scenario: 目录命名不使用中文或大写

- **WHEN** 检查 `docs/content/` 下任一一级子目录名
- **THEN** 该目录名只包含小写英文字母、数字、连字符 `-`,不出现中文字符、空格、下划线或驼峰命名

### Requirement: 14 个固定章节 + 附录占位齐备

系统 SHALL 在 `docs/content/` 下一次性创建以下 14 个章节目录 + `99-appendix` 目录,每个目录包含一个占位 `_index.md`,正文部分仅保留一行 TODO 注释或空行。章节清单与编号 SHALL 完全一致,且采用"上篇理论指导思想(01-07)+ 下篇落地实践路径(08-14)+ 附录(99)"的双篇结构:

**上篇·理论指导思想:**
1. `01-background` — 背景与 Harness 定义
2. `02-harness-core` — Harness 核心思想：评估驱动的自主 Agent 闭环
3. `03-devops-foundation` — DevOps 与持续交付理论
4. `04-sre-foundation` — SRE 与可靠性工程理论
5. `05-collaboration-paradigm` — 人机协作范式：验收驱动模型
6. `06-closed-loop-framework` — 全生命周期闭环框架设计
7. `07-agent-roles` — Agent 角色体系与分工模型

**下篇·落地实践路径:**
8. `08-overall-architecture` — 总体架构：AI 执行引擎与人类验收层
9. `09-requirement-phase` — 需求阶段：AI 主导需求工程
10. `10-design-phase` — 架构设计阶段：AI 主导技术方案
11. `11-development-phase` — 研发阶段：AI 主导编码实现
12. `12-testing-phase` — 测试阶段：AI 主导质量验证
13. `13-deployment-ops-phase` — 部署与运维阶段：AI 主导系统保障
14. `14-review-evolution-phase` — 归档复盘与 AI 持续进化

**附录:**
15. `99-appendix` — 附录

#### Scenario: 14 章 + 附录目录全部存在

- **WHEN** 检查 `docs/content/` 下的一级子目录
- **THEN** 上述 15 个目录全部存在,数量为 15,目录名与编号一一对应,不多不少

#### Scenario: 每个章节目录包含 _index.md 占位

- **WHEN** 对上述任一章节目录 `<chapter>/` 检查
- **THEN** 该目录下存在 `_index.md`,文件大小不为 0,且其 front matter 中 `title` 字段值与上方"中文章节标题"完全一致

### Requirement: 章节 _index.md 的 front matter 约定

每个章节占位 `_index.md` 的 front matter SHALL 包含以下字段:`title`(中文章节标题)、`weight`(等于章节编号 × 10 的整数,如 `01-background` 对应 `weight = 10`)、`bookCollapseSection = true`、`draft = false`。

#### Scenario: weight 值与章节顺序一致

- **WHEN** 读取所有 15 个 `_index.md` 的 `weight` 字段并按升序排序
- **THEN** 排序结果对应的目录名顺序等于上一条 Requirement 中给出的章节清单顺序

#### Scenario: 章节根使用 bookCollapseSection 折叠子页

- **WHEN** 检查任一章节 `_index.md` 的 front matter
- **THEN** 包含 `bookCollapseSection = true`,使 Book 主题在左侧目录树中默认折叠该章节下的子页面

#### Scenario: 章节占位不标记为 draft

- **WHEN** 检查任一章节 `_index.md` 的 front matter
- **THEN** `draft = false`(占位本身需要在预览/线上中可见,以呈现完整目录骨架)

### Requirement: 章节正文仅占位,不写实际内容

系统 SHALL 在每个章节占位 `_index.md` 的正文部分(front matter 之后)仅保留一行注释 `<!-- TODO: 在后续 change 中填充本章正文 -->` 或同等长度的占位。本次 change 范围内 SHALL NOT 写入任何 Harness 工程实施方案的实际段落、列表、图表。

#### Scenario: 占位文件正文不超过 5 行非空内容

- **WHEN** 对任一章节 `_index.md` 去除 front matter 后统计非空行数
- **THEN** 非空行数不超过 5,且其中至少一行为 `TODO` 注释或等价占位

### Requirement: 章节骨架与正文写作分离

后续每一章正文的填充 SHALL 通过独立的 OpenSpec change 进行(每章一次或按主题合并),本次 change SHALL NOT 在 `proposal.md` / `tasks.md` 中预先承诺正文写作时间表或负责人。

#### Scenario: 本次 change 不包含正文交付物

- **WHEN** 检查本次 change 的 `tasks.md` 全部任务标题
- **THEN** 任意任务的标题或描述不出现"撰写第 X 章正文"、"完成 Y 章节内容"等正文写作语义;只允许出现"创建占位"、"生成 _index.md"等骨架级动作

### Requirement: "Harness" 一词采用 Anthropic 论文语义

本计划中"Harness"一词 SHALL 采用 Anthropic [Harness Design for Long-Running Application Development](https://www.anthropic.com/engineering/harness-design-long-running-apps) 一文中的定义——围绕长时运行 LLM 智能体的编排脚手架(planner / generator / evaluator + sprint 契约 + 上下文管理 + 工具集成),而 SHALL NOT 指代 harness.io 公司提供的 CI/CD / Feature Flags / Cloud Cost 等 SaaS 产品。

#### Scenario: 章节命名不引入 CI/CD 平台特征词

- **WHEN** 检查 15 个章节目录名与中文标题
- **THEN** 不出现 `pipelines`、`feature-flags`、`ccm`、`sto`、`cd-modules`、`harness-platform` 等指向 harness.io SaaS 产品的术语;上篇围绕智能体编排理论与工程体系展开,下篇围绕软件全生命周期自动化展开

#### Scenario: 双篇结构有明确边界

- **WHEN** 把 15 个目录按编号铺平
- **THEN** `01-07` 为上篇理论指导思想,`08-14` 为下篇落地实践路径,`99` 为附录,上篇与下篇之间在编号上连续且无遗漏
