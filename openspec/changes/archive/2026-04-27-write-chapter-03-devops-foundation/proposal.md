## Why

`03-devops-foundation` 章节目前仅为占位状态。前两章已建立 Harness 的问题意识和核心思想，第三章需要将 Harness 与软件工程的成熟理论对接——说明 Harness 不是孤立的框架，而是 DevOps 持续交付理念的延伸和强化。

## What Changes

- 将 `docs/content/docs/03-devops-foundation/_index.md` 的占位内容替换为完整的第三章正文
- 精炼版篇幅（约 1500-2000 中文字符），聚焦 Harness × DevOps 交汇点
- 结构 ACB，占比 8:1:1：
  - A（80%）：Harness 是 DevOps 的终极形态——反馈环从小时级压缩到分钟级
  - C（10%）：DevOps 是 Harness 的土壤——Harness 依赖 DevOps 基础设施
  - B（10%）：需要重新诠释的实践——代码审查→Evaluator 验收、人工审批→自动门控
- 弱化 Evaluator 验收与 DevOps 自动化测试的层次区分，强调融合
- 保持 Hugo Book 主题的 front matter 不变

## Capabilities

### New Capabilities
- （无新建能力）

### Modified Capabilities
- （无修改能力）

## Impact

- **受影响文件**: `docs/content/docs/03-devops-foundation/_index.md`
- **构建影响**: 无——Hugo 构建流程不变
- **下游影响**: 为 04 章 SRE 理论和 08-14 章落地实践提供 DevOps 理论基础
