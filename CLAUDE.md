# Harness 工程落地方案 —— 协作指南

基于 Anthropic Agent Harness 论文的工程实践文档站点，使用 Hugo + hugo-book 主题构建。

## 项目结构

```
docs/                          # Hugo 文档站点
  content/docs/
    01-background/             # 第一章：背景与问题意识
    02-harness-core/           # 第二章：Harness 核心思想
    03-devops-foundation/      # 第三章：DevOps 基础
    04-sre-foundation/         # 第四章：SRE 与可靠性工程
    05-collaboration-paradigm/ # 第五章：人机协作范式
    06-closed-loop-framework/  # 第六章：全生命周期闭环框架
    ...                        # 后续章节
  themes/hugo-book/            # 主题
openspec/                      # OpenSpec 变更管理
  specs/                       # 能力规格（内容标准）
  changes/                     # 活跃的变更
  changes/archive/             # 已归档的变更
```

## 章节写作规范

### 标准工作流程

每章正文开发遵循以下流程：

1. **用户给出章节框架**（大纲、核心论点、关键案例）
2. **`/opsx:propose <name>`** —— 创建 openspec change，生成 proposal / design / specs / tasks
3. **`/opsx:apply <name>`** —— 按 tasks 逐条实现正文内容
4. **`/opsx:archive <name>`** —— 归档 change，同步 specs 到 `openspec/specs/`
5. **`git commit && git push`** —— 提交并推送

### 前后章衔接检查

撰写新章前必须读取：
- **前一章末尾**（最后 2-3 段）—— 新章开头应以"追问"方式承接
- **后一章开头**（如果已存在）—— 新章结尾应能自然引出

衔接句式模板：
- 承接：`## 第N章的追问` + 提炼上一章的未解决问题
- 引出：本章小结最后一句指向下一章主题

### 术语一致性

首次出现的专业术语必须附带通俗解释，格式为"专业名词（通俗解释）"：

| 术语 | 首次出现格式 |
|------|-------------|
| Planner | Planner（设计规划） |
| Generator | Generator（实现者） |
| Evaluator | Evaluator（验收方） |
| 验收标准 | 验收标准（完成定义） |
| 轮次 | 轮次（sprint） |
| 信息交接 | 信息交接（Context Handoff） |

**注意**：术语的通俗解释在全书中必须保持一致。如 Planner 已从"任务拆解"改为"设计规划"，后续章节不得回退到旧解释。

### 内容说明格式

每章末尾必须包含 `**本章内容说明**` 段落，使用以下四类标签：

- **论文原意**：源自 Anthropic Harness 论文的内容
- **作者分析**：基于论文框架的分析推导
- **工程扩展**：落地实践补充，非论文原文
- **虚构示例**：构造的说明性场景

### 质量检查清单（提交前）

- [ ] 前后章衔接自然（开头承接、结尾引出）
- [ ] 专业术语首次出现附带通俗解释，且与已有章节一致
- [ ] 末尾包含"本章内容说明"段落，四类标注完整
- [ ] Hugo 本地构建通过（`make preview` 或 `hugo --gc`）
- [ ] openspec tasks 全部完成（如有）
- [ ] 提交信息遵循 `feat(chXX): ...` / `fix(chXX): ...` 格式

## 内容风格指南

### 叙事策略

- **第一章**：叙事化风格，面向决策者，避免工程术语
- **第二章起**：技术论述 + 通俗解释，面向技术管理者和工程师
- **每章开头**：以"追问"方式承接上一章，建立阅读动势
- **论证方式**：理论映射（DevOps/SRE）+ 类比（数控机床）+ 具体案例

### 章节结构模板

```markdown
## 第N-1章的追问
[承接段落]

## M.M 小节标题
[正文...]

### M.M.M 子小节
[正文...]

## M.5 本章小结
[核心脉络回顾 + 全书逻辑定位 + 下一章引出]

---

**本章内容说明**
- **论文原意**：...
- **作者分析**：...
- **工程扩展**：...
- **虚构示例**：...
```

## openspec 使用约定

### 变更命名

章节开发使用统一前缀：`write-chapter-XX-<kebab-topic>`

示例：
- `write-chapter-05-collaboration-paradigm`
- `write-chapter-06-closed-loop-framework`

### specs 同步策略

章节内容规格（content specs）属于项目资产，归档时应同步到 `openspec/specs/`：
- 新章节的能力规格 → 新增到 `openspec/specs/<capability>/spec.md`
- 已有章节的修改 → 创建 delta spec 并同步

### 归档后清理

归档完成后确认：
- `openspec/changes/` 下无残留目录（全部移至 `archive/`）
- `openspec/specs/` 已包含最新同步的规格
