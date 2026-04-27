+++
title = 'DevOps 与持续交付理论'
weight = 30
bookCollapseSection = true
draft = false
+++

## DevOps 的终极问题

DevOps 的核心理念可以用一句话概括：**让软件交付像流水线一样自动化、可重复、可预期。** 持续集成（CI）确保每次代码变更都能自动构建和测试，持续交付（CD）确保通过测试的代码能自动部署到生产环境。这套方法论的终极目标是压缩"从需求到上线"的时间，同时保证质量不降。

但 DevOps 有一个隐形的天花板——**人仍然是反馈环中的阻塞点。**

想象一个典型的 DevOps 反馈环：程序员写完代码，提交到代码仓库，CI 服务器自动构建和运行单元测试，测试通过后部署到预发布环境，QA 工程师手动验证功能，发现 bug，提交工单，程序员接收工单、定位问题、修复代码，再次提交——整个循环走完，短则几小时，长则一两天。

这个反馈环中，CI/CD 工具自动化了"构建、测试、部署"，但"编码"和"验收"仍然依赖人。人需要理解需求、编写代码、验证结果、修复问题。当需求复杂到需要多轮迭代时，这个"人参与"的环节就成了速度的天花板。

Harness 的出现，把这个天花板打破了。

## Harness：DevOps 反馈环的极致延伸

在 Harness 的框架中，反馈环的运转方式发生了根本性的变化。

**传统 DevOps 反馈环：**

{{< mermaid >}}
flowchart LR
    A[人写代码] --> B[提交]
    B --> C[CI构建]
    C --> D[自动化测试]
    D --> E[人验收]
    E -->|通过| F[部署]
    E -->|不通过| G[人修复]
    G --> B
    
    style A fill:#ffcccc,stroke:#cc0000,stroke-width:2px
    style E fill:#ffcccc,stroke:#cc0000,stroke-width:2px
    style G fill:#ffcccc,stroke:#cc0000,stroke-width:2px
    style F fill:#ccffcc,stroke:#009900,stroke-width:2px
{{< /mermaid >}}

**Harness 反馈环：**

{{< mermaid >}}
flowchart LR
    P[Planner拆解] --> G[Generator编码]
    G --> E[Evaluator评估]
    E -->|不通过| G2[Generator完善]
    G2 --> E
    E -->|通过| D[部署到环境]
    D --> M[监控反馈]
    M -->|发现问题| G3[回流完善]
    G3 --> E
    M -->|正常| H[程序员门禁]
    
    style P fill:#cce5ff,stroke:#0066cc,stroke-width:2px
    style G fill:#cce5ff,stroke:#0066cc,stroke-width:2px
    style E fill:#cce5ff,stroke:#0066cc,stroke-width:2px
    style G2 fill:#cce5ff,stroke:#0066cc,stroke-width:2px
    style G3 fill:#cce5ff,stroke:#0066cc,stroke-width:2px
    style H fill:#ffcccc,stroke:#cc0000,stroke-width:2px
{{< /mermaid >}}

两者的差异不在于"自动化的程度"，而在于**"反馈的密度"**。

传统 DevOps 中，一个功能的编码、测试、修复可能只走一轮或两轮循环，因为人的参与限制了循环次数。Harness 中，Evaluator 的评估和 Generator 的完善可以在**分钟级**完成一轮，一个复杂功能可能经历多轮"评估→完善→再评估→再完善"，直到 Evaluator 的验收标准全部通过。

但这个反馈环的密度不是没有代价的。Anthropic 团队的实测数据显示，运行一次完整的 Harness 流程的成本约为 **200 美元**，而同等任务由单个 AI 独立完成的成本仅为 **9 美元**——Harness 的反馈环密度是用**20 倍以上的成本**换来的。在实际工程中，这种成本权衡需要被正视：前端任务通常经历 **5 到 15 轮**评估迭代，每轮都消耗 token；如果评估标准设置过于严苛，或者 Generator 反复无法通过某项验收，成本会迅速攀升。

因此，Harness 的评估循环不是无限进行的。论文中的实践表明，评估分数会随着迭代逐步提升，直到进入**平台期**——此时继续投入更多轮次的收益递减，系统应当停止循环、进入下一环节。此外，每个验收标准都有**硬阈值**，如果某项指标始终无法达标，sprint 会被判定为失败，而不是无限重试。这种"有节制的自动化"是 Harness 落地的关键：它追求的不是"无限完美的代码"，而是"在成本可控范围内达到可接受质量的代码"。

更关键的是，Harness 的反馈环**不止于编码阶段**。代码通过 Evaluator 验收后，会部署到测试环境或预发布环境，运行时的监控数据、日志、性能指标会回流到系统——如果发现异常，Generator 会基于这些反馈自动修复，Evaluator 再次验证。这个"部署→观测→反馈→修复"的闭环，把 DevOps 的反馈环从"开发阶段"延伸到了"运行阶段"。

这不是说 Harness 替代了 DevOps——恰恰相反，Harness 是 DevOps 逻辑的**自然延伸**。DevOps 说"自动化一切可以自动化的事"，Harness 说"编码、验收、修复、部署反馈也是可以自动化的事"。当反馈环中的最后一个"人参与"环节被消除时，DevOps 的愿景才真正完整。

## DevOps 是 Harness 的土壤

Harness 不是从零开始构建的独立王国，它站在 DevOps 已经铺好的基础设施之上。

没有代码仓库和版本控制，Generator 就无法管理代码变更；没有 CI/CD 流水线，Evaluator 就无法自动构建和运行测试；没有测试环境和部署机制，Evaluator 就无法验证代码在真实运行时的表现；没有监控和日志系统，Harness 就无法获取部署后的运行反馈。Harness 的三个角色（Planner、Generator、Evaluator）需要 DevOps 提供的"舞台"才能运转。

换句话说，Harness 解决的是"**谁**在编码、验收和修复"的问题，DevOps 解决的是"**如何**构建、测试、部署和监控"的问题。两者是互补关系，不是替代关系。

## 程序员：最后一道门禁

当 Harness 的 AI 层完成多轮"评估→完善"循环并收敛后，代码才会进入**人工门禁**环节。这个环节的角色不是"验收功能是否正确"——Evaluator 已经完成了这个功能层面的验证——而是**程序员对工程质量的最终把关**。

具体来说，程序员需要结合 AI 提供的全套产出物进行确认：

- **设计文档**：确认技术方案的可行性、安全性和与现有架构的兼容性；
- **代码**：审查可读性、可维护性，排查 Evaluator 可能遗漏的安全隐患；
- **测试文档**：验证覆盖测试、功能测试、自动化测试、性能测试、回归测试的完整性；
- **监控配置**：确认部署后的观测维度是否完备，告警阈值是否合理。

对于金融、医疗等故障零容忍的系统，程序员的最终确认不是"可选项"，而是"必选项"。AI 可以加速迭代、压缩反馈周期，但**对线上故障的最终责任，仍然由人承担**。

---

**本章内容说明**

- **论文原意**：Harness 的 AI 自治框架（Planner / Generator / Evaluator 三角色、验收标准协商、信息交接），以及实测成本数据（Solo run $9、Full harness $200、约 20 倍成本差异、前端 5-15 轮迭代、硬阈值停止条件、分数平台期），源自 Anthropic《Harness Design for Long-Running Application Development》。
- **作者分析**："Harness 是 DevOps 反馈环的极致延伸"的论断、传统 DevOps 与 Harness 反馈环的速度对比、多轮"评估→完善"循环、部署后监控反馈闭环、DevOps 与 Harness 的互补关系论述，为本文作者基于论文框架和 DevOps 理念的分析推导。反馈环时间对比（小时级/天级 vs 分钟级）为概念性量级描述，非精确测量数据。
- **工程扩展**："程序员作为最后一道门禁"的论述，以及覆盖测试、功能测试、自动化测试、性能测试、回归测试等具体质量把关维度，是基于论文框架和 DevOps 工程实践结合的落地推导，非论文原文。
