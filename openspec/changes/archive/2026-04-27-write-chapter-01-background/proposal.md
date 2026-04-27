## Why

`01-background` 章节目前仅为占位状态（`_index.md` 中只有 TODO 注释）。作为整本书的入口章节，它需要回答"为什么值得读下去"这个问题——通过叙事化的痛点故事，让管理层/决策者感受到 AI 辅助长时开发任务中的真实困境，从而建立阅读动机。

## What Changes

- 将 `docs/content/docs/01-background/_index.md` 的占位内容替换为完整的第一章正文
- 正文采用叙事风格，面向管理层/决策者视角
- 核心策略：**只提问题，不讲原理**——具体什么是 Harness、怎么工作，留给第二章
- 引用 Claude Code 相关实践案例，铺垫"工程化落地"难题
- 保持 Hugo Book 主题的 front matter 不变（title、weight、bookCollapseSection、draft）
- 正文控制在合理长度（约 1500-2500 中文字符），避免过度展开

## Capabilities

### New Capabilities
<!-- 本次 change 为纯文档内容创作，不引入新的系统能力或功能变更。章节骨架已在 harness-plan-outline 中定义，本次仅填充占位内容。 -->
- （无新建能力）

### Modified Capabilities
<!-- 本次 change 不修改任何已有 spec 中的 REQUIREMENTS，仅对 harness-plan-outline 中已创建的占位文件进行内容填充 -->
- （无修改能力）

## Impact

- **受影响文件**: `docs/content/docs/01-background/_index.md`
- **构建影响**: 无——Hugo 构建流程不变，仅 Markdown 正文内容变更
- **下游影响**: 为后续 `02-harness-core` 章节做铺垫，建立问题意识
