# AI Service - 研发全流程 AI 服务平台设计文档

## 目标

构建一个生产级别的 Python AI 服务，接收 GitLab Webhook，对接 Claude SDK，支持软件研发全流程场景（代码评审、需求拆分、Bugfix），所有 Claude 输出在 Docker 沙箱中执行验证。

## 架构

采用轻量插件化架构：核心框架负责 GitLab Webhook 接收、认证、路由分发；每个场景作为独立插件实现统一接口；Claude SDK 统一封装；Docker 沙箱执行所有 AI 输出。

## 技术栈

- Python 3.12 + Flask + Gunicorn
- uv（包管理）
- Anthropic SDK（Claude API）
- Docker（沙箱执行）
- pytest（测试）
- Pydantic Settings（配置管理）

## 系统架构图

```
┌─────────────────────────────────────────────────────────┐
│                    GitLab (Source)                       │
│         MR created / Issue opened / Bug reported        │
└───────────────────────┬─────────────────────────────────┘
                        │ Webhook POST
                        ▼
┌─────────────────────────────────────────────────────────┐
│              Flask Application (Gunicorn)                │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  │
│  │   Webhook    │  │   Scenario   │  │    Claude    │  │
│  │   Receiver   │──│   Dispatcher │──│   Service    │  │
│  │  (GitLab Auth)│  │ (Route to    │  │(Anthropic    │  │
│  │              │  │  Scenario)   │  │  SDK Wrapper)│  │
│  └──────────────┘  └──────────────┘  └──────────────┘  │
│                        │                                │
│         ┌──────────────┼──────────────┐                │
│         ▼              ▼              ▼                │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐      │
│  │ Code Review │ │Story Split  │ │   Bugfix    │      │
│  │  Scenario   │ │  Scenario   │ │  Scenario   │      │
│  └──────┬──────┘ └─────────────┘ └──────┬──────┘      │
│         │                                 │            │
│         │    ┌─────────────────────┐     │            │
│         └───►│    Sandbox Layer    │◄────┘            │
│              │  (Code Execution)   │                  │
│              │  • Docker 容器      │                  │
│              │  • 资源限制         │                  │
│              │  • 网络隔离         │                  │
│              │  • 超时 60s         │                  │
│              └──────────┬──────────┘                  │
└─────────────────────────┼─────────────────────────────┘
                          │
                          ▼
                ┌─────────────────┐
                │   GitLab API    │  ← 回写结果
                └─────────────────┘
```

## 目录结构

```
ai-service/
├── pyproject.toml              # uv 配置，依赖管理
├── uv.lock                     # 锁定文件
├── .python-version             # Python 3.12
├── .env.example                # 环境变量示例
├── README.md                   # 项目文档
├── Dockerfile                  # 服务容器化
├── docker-compose.yml          # 本地开发/部署
├── gunicorn.conf.py            # Gunicorn 配置
├── src/
│   └── ai_service/
│       ├── __init__.py
│       ├── app.py              # Flask 应用工厂
│       ├── config.py           # Pydantic Settings 配置
│       ├── extensions.py       # 扩展初始化
│       ├── routes/
│       │   ├── __init__.py
│       │   ├── health.py       # 健康检查 /health
│       │   ├── metrics.py      # Prometheus /metrics
│       │   └── webhook.py      # GitLab Webhook /webhook/gitlab
│       ├── middleware/
│       │   ├── __init__.py
│       │   └── gitlab_auth.py  # GitLab Token 认证
│       ├── core/
│       │   ├── __init__.py
│       │   ├── dispatcher.py   # 场景分发器
│       │   └── scenario.py     # 场景抽象基类
│       ├── scenarios/
│       │   ├── __init__.py
│       │   ├── code_review.py  # 代码评审场景
│       │   ├── story_split.py  # 需求拆分场景
│       │   └── bugfix.py       # Bugfix 场景
│       ├── services/
│       │   ├── __init__.py
│       │   ├── claude.py       # Claude SDK 封装
│       │   ├── gitlab.py       # GitLab API 封装
│       │   └── sandbox.py      # Docker 沙箱执行器
│       └── utils/
│           ├── __init__.py
│           └── logger.py       # 结构化日志配置
├── sandbox/
│   └── Dockerfile              # 沙箱容器镜像
├── tests/
│   ├── __init__.py
│   ├── conftest.py             # pytest 全局配置
│   ├── unit/
│   │   ├── __init__.py
│   │   ├── test_config.py
│   │   ├── test_gitlab_auth.py
│   │   ├── test_dispatcher.py
│   │   ├── test_claude_service.py
│   │   ├── test_sandbox.py
│   │   └── test_scenarios/
│   │       ├── test_code_review.py
│   │       ├── test_story_split.py
│   │       └── test_bugfix.py
│   └── integration/
│       ├── __init__.py
│       └── test_webhook_flow.py
└── docs/
    └── superpowers/
        ├── specs/              # 设计文档
        └── plans/              # 实现计划
```

## 组件设计

### 1. 应用工厂（app.py）

创建 Flask 应用实例，注册蓝图，初始化扩展。使用应用工厂模式便于测试。

### 2. GitLab Webhook 认证（middleware/gitlab_auth.py）

- 验证 `X-Gitlab-Token` header
- 验证通过后才进入场景分发
- 认证失败返回 401，记录安全日志

### 3. 场景分发器（core/dispatcher.py）

- 根据 `X-Gitlab-Event` header 和 payload 内容决定调用哪个场景
- 配置映射：`event_type + 条件 → scenario_class`
- 未匹配的事件返回 200（避免 GitLab 重试）

### 4. 场景基类（core/scenario.py）

```python
class Scenario(ABC):
    @property
    @abstractmethod
    def name(self) -> str: ...

    @abstractmethod
    def can_handle(self, event_type: str, payload: dict) -> bool: ...

    @abstractmethod
    def handle(self, payload: dict) -> ScenarioResult: ...
```

### 5. Claude 服务（services/claude.py）

- 封装 Anthropic SDK
- 统一 prompt 模板管理
- 错误重试（指数退避，3 次）
- Token 使用量记录

### 6. GitLab 服务（services/gitlab.py）

- 封装 GitLab API 调用
- 获取 MR diff
- 创建评论/议题
- 错误重试

### 7. 沙箱执行器（services/sandbox.py）

```python
class SandboxExecutor:
    def execute(
        self,
        code: str,
        language: str,
        timeout: int = 60,
        memory_limit: str = "512m",
        network: bool = False,
        allowed_paths: list[str] | None = None,
    ) -> SandboxResult: ...
```

- Docker 容器执行
- 资源限制：60s 超时、512MB 内存
- 默认禁用网络
- tmpfs 挂载工作目录

### 8. 配置管理（config.py）

Pydantic Settings 管理环境变量：
- `GITLAB_WEBHOOK_TOKEN`
- `GITLAB_API_TOKEN`
- `GITLAB_URL`
- `ANTHROPIC_API_KEY`
- `ANTHROPIC_MODEL`（默认 claude-sonnet-4-6）
- `SANDBOX_TIMEOUT`
- `SANDBOX_MEMORY_LIMIT`
- `LOG_LEVEL`

## 场景详细设计

### 代码评审场景（code_review.py）

**触发条件：**
- Event: `Merge Request Hook`
- Action: `open` 或 `update`

**处理流程：**
1. 从 payload 提取 project_id 和 merge_request_iid
2. 调用 GitLab API 获取 MR diff
3. 构建代码评审 prompt（包含 diff 和评审标准）
4. 调用 Claude API 获取评审意见
5. 将评审意见写入沙箱执行静态检查（ruff、mypy）
6. 收集沙箱执行结果
7. 将评审意见 + 沙箱验证结果以评论形式写入 MR

**Prompt 模板：**
```
你是一位资深代码审查员。请对以下代码变更进行评审：

【变更内容】
{diff}

【评审维度】
1. 代码正确性和潜在 Bug
2. 性能问题
3. 安全漏洞
4. 代码风格和可读性
5. 测试覆盖

【输出格式】
- 严重问题（blocking）
- 建议改进（suggestion）
- 正面反馈（praise）

如有修复建议，请提供具体代码示例。
```

### 需求拆分场景（story_split.py）

**触发条件：**
- Event: `Issue Hook`
- Action: `open`
- Label: 包含 `needs-split`

**处理流程：**
1. 从 payload 提取 issue 标题和描述
2. 构建需求拆分 prompt
3. 调用 Claude API 获取拆分结果
4. 将拆分结果写入沙箱做格式验证（JSON Schema 校验）
5. 调用 GitLab API 创建子 issues

**Prompt 模板：**
```
你是一位资深产品经理。请将以下需求拆分为可独立开发、测试、交付的子任务：

【需求】
标题：{title}
描述：{description}

【拆分要求】
1. 每个子任务应有明确的目标和验收标准
2. 子任务之间依赖关系清晰
3. 每个子任务应在 3 天内可完成
4. 使用用户故事格式（As...I want...so that...）

【输出格式】
以 JSON 数组形式输出，每个元素包含：
- title: 子任务标题
- description: 详细描述
- acceptance_criteria: 验收标准列表
- estimated_hours: 预估工时
```

### Bugfix 场景（bugfix.py）

**触发条件：**
- Event: `Issue Hook`
- Action: `open`
- Label: 包含 `bug`

**处理流程：**
1. 从 payload 提取 issue 标题、描述、复现步骤
2. 如包含代码仓库信息，获取相关文件内容
3. 构建 Bug 分析 prompt
4. 调用 Claude API 获取根因分析和修复建议
5. 将修复代码写入沙箱执行验证
6. 将分析结果和验证结果以评论形式写入 Issue

**Prompt 模板：**
```
你是一位资深开发工程师。请分析以下 Bug 并提供修复建议：

【Bug 描述】
标题：{title}
描述：{description}
复现步骤：{reproduction_steps}

【相关代码】
{related_code}

【分析要求】
1. 根因分析（为什么出现这个 Bug）
2. 修复方案（具体代码修改）
3. 预防措施（如何避免类似 Bug）
4. 测试建议（如何验证修复）

【输出格式】
- 根因：...
- 修复代码：```language\n...\n```
- 预防措施：...
- 测试建议：...
```

## 数据流设计

### 代码评审场景完整数据流

```
GitLab MR Webhook
       │
       ▼
┌─────────────┐     ┌─────────────┐     ┌─────────────────┐
│ 1. Receive  │────►│ 2. Fetch    │────►│ 3. Build Prompt │
│    Webhook  │     │    MR Diff  │     │                 │
└─────────────┘     └─────────────┘     └────────┬────────┘
                                                  │
                                                  ▼
┌─────────────┐     ┌─────────────┐     ┌─────────────────┐
│ 6. Post     │◄────│ 5. Execute  │◄────│ 4. Call Claude  │
│    Comment  │     │    in Sandbox     │     │    API      │
└─────────────┘     └─────────────┘     └─────────────────┘
```

1. **接收 Webhook**：验证 GitLab Token，解析 `Merge Request Hook` 事件
2. **获取 MR Diff**：调用 GitLab API 获取变更内容
3. **构建 Prompt**：将 diff + 评审要求组装成结构化 prompt
4. **调用 Claude**：通过 Anthropic SDK 获取评审结果
5. **沙箱执行**：将 Claude 的输出写入沙箱容器执行验证
6. **回写结果**：将评审意见 + 沙箱验证结果以评论形式写入 MR

## 错误处理策略

| 错误类型 | 处理策略 |
|----------|----------|
| GitLab Webhook 认证失败 | 返回 401，记录安全日志 |
| GitLab API 调用失败 | 重试 3 次（指数退避），失败则记录错误 |
| Claude API 限流/超时 | 重试 3 次，失败则标注"AI 服务暂时不可用" |
| 沙箱执行超时（60s） | 强制终止容器，标注"代码执行超时" |
| 沙箱执行异常 | 收集 stderr，作为评审的一部分回写 |
| 未匹配的场景 | 返回 200 OK（避免 GitLab 重试） |

## API 契约

### Webhook 接收端点

```http
POST /webhook/gitlab
Headers:
  X-Gitlab-Event: Merge Request Hook
  X-Gitlab-Token: <secret>
Body:
  GitLab webhook payload
Response:
  200 OK - 处理成功或无需处理
  401 Unauthorized - 认证失败
```

### 健康检查

```http
GET /health
Response:
  {"status": "ok", "version": "1.0.0"}
```

### Prometheus Metrics

```http
GET /metrics
```

暴露指标：
- `webhook_requests_total` - Webhook 请求总数
- `webhook_duration_seconds` - Webhook 处理耗时
- `claude_api_calls_total` - Claude API 调用次数
- `claude_api_duration_seconds` - Claude API 调用耗时
- `sandbox_executions_total` - 沙箱执行次数
- `sandbox_execution_duration_seconds` - 沙箱执行耗时

## 部署架构

```
┌─────────────────────────────────────────┐
│              Docker Compose             │
│                                         │
│  ┌─────────────┐    ┌─────────────┐    │
│  │   Flask App │◄──►│   Sandbox   │    │
│  │   (Gunicorn)│    │   Network   │    │
│  │   Port 5000 │    │   (isolated)│    │
│  └──────┬──────┘    └─────────────┘    │
│         │                               │
│         │    ┌─────────────────┐        │
│         └───►│  Sandbox Volumes│        │
│              │  (tmpfs, rw)    │        │
│              └─────────────────┘        │
└─────────────────────────────────────────┘
```

### 生产配置

- Gunicorn + Gevent worker（支持并发 webhook 处理）
- Docker socket 挂载（Flask 调用 Docker API 创建沙箱容器）
- 日志输出到 stdout（容器化标准做法）
- 环境变量注入敏感信息（API keys）

## 测试策略

| 测试类型 | 覆盖内容 |
|----------|----------|
| 单元测试 | 场景逻辑、Claude prompt 构建、沙箱命令生成 |
| 集成测试 | Flask webhook 端到端（mock GitLab API + mock Claude API） |
| 沙箱测试 | 验证 Docker 沙箱隔离性、资源限制生效 |

## 迭代计划

### 迭代 1（当前）
- 核心框架（Flask 应用、GitLab 认证、场景分发）
- Claude SDK 封装
- Docker 沙箱执行器
- 3 个场景：代码评审、需求拆分、Bugfix
- 单元测试 + 集成测试

### 迭代 2（未来）
- 架构设计场景
- 架构评审场景
- 代码开发场景
- 测试生成场景
- 部署辅助场景
- 合并评估场景
- 异步队列支持（Redis + Celery）
- 流式响应支持
