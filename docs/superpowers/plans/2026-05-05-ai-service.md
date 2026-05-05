# AI Service 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 构建生产级 Python AI 服务，接收 GitLab Webhook，对接 Claude SDK，支持代码评审、需求拆分、Bugfix 三个场景，所有 AI 输出在 Docker 沙箱中执行验证。

**Architecture:** Flask 应用工厂模式 + 插件化场景系统。核心框架负责 Webhook 接收与认证，Dispatcher 路由到对应场景，Claude Service 统一封装 AI 调用，Sandbox 执行器在 Docker 容器中验证 AI 输出。

**Tech Stack:** Python 3.12, Flask, Gunicorn, uv, Anthropic SDK, Docker SDK, Pydantic Settings, pytest

---

## 文件结构

```
ai-service/
├── pyproject.toml
├── uv.lock
├── .python-version
├── .env.example
├── Dockerfile
├── docker-compose.yml
├── gunicorn.conf.py
├── src/ai_service/
│   ├── __init__.py
│   ├── app.py
│   ├── config.py
│   ├── extensions.py
│   ├── routes/__init__.py, health.py, metrics.py, webhook.py
│   ├── middleware/__init__.py, gitlab_auth.py
│   ├── core/__init__.py, dispatcher.py, scenario.py
│   ├── scenarios/__init__.py, code_review.py, story_split.py, bugfix.py
│   ├── services/__init__.py, claude.py, gitlab.py, sandbox.py
│   └── utils/__init__.py, logger.py
├── sandbox/Dockerfile
└── tests/
    ├── conftest.py
    ├── unit/test_config.py, test_gitlab_auth.py, test_dispatcher.py,
    │         test_claude_service.py, test_sandbox.py,
    │         test_scenarios/test_code_review.py, test_story_split.py, test_bugfix.py
    └── integration/test_webhook_flow.py
```

---

### Task 1: 项目初始化

**Files:**
- Create: `ai-service/pyproject.toml`
- Create: `ai-service/.python-version`
- Create: `ai-service/.env.example`

- [ ] **Step 1: 创建项目目录并初始化 uv**

```bash
mkdir -p ai-service && cd ai-service
uv init --no-workspace
echo "3.12" > .python-version
```

- [ ] **Step 2: 写入 pyproject.toml 依赖**

```toml
[project]
name = "ai-service"
version = "0.1.0"
requires-python = ">=3.12"
dependencies = [
    "flask>=3.1",
    "gunicorn[gevent]>=23.0",
    "anthropic>=0.40",
    "docker>=7.0",
    "pydantic-settings>=2.7",
    "prometheus-flask-exporter>=0.23",
    "requests>=2.32",
    "tenacity>=9.0",
]

[project.optional-dependencies]
dev = ["pytest>=8.0", "pytest-flask>=1.3", "pytest-mock>=3.14"]

[build-system]
requires = ["hatchling"]
build-backend = "hatchling.build"

[tool.hatch.build.targets.wheel]
packages = ["src/ai_service"]
```

- [ ] **Step 3: 写入 .env.example**

```bash
cat > .env.example << 'EOF'
GITLAB_WEBHOOK_TOKEN=your-webhook-secret
GITLAB_API_TOKEN=your-gitlab-token
GITLAB_URL=https://gitlab.example.com
ANTHROPIC_API_KEY=your-anthropic-key
ANTHROPIC_MODEL=claude-sonnet-4-6
SANDBOX_TIMEOUT=60
SANDBOX_MEMORY_LIMIT=512m
LOG_LEVEL=INFO
EOF
```

- [ ] **Step 4: 安装依赖**

```bash
cd ai-service && uv sync --extra dev
```

Expected: 依赖安装成功，生成 uv.lock

- [ ] **Step 5: Commit**

```bash
git add ai-service/pyproject.toml ai-service/.python-version ai-service/.env.example ai-service/uv.lock
git commit -m "feat: initialize ai-service project with uv"
```

---

### Task 2: 配置与日志

**Files:**
- Create: `ai-service/src/ai_service/__init__.py`
- Create: `ai-service/src/ai_service/config.py`
- Create: `ai-service/src/ai_service/utils/__init__.py`
- Create: `ai-service/src/ai_service/utils/logger.py`
- Create: `ai-service/tests/__init__.py`
- Create: `ai-service/tests/unit/__init__.py`
- Create: `ai-service/tests/unit/test_config.py`

- [ ] **Step 1: 写失败测试**

```python
# tests/unit/test_config.py
import pytest
from pydantic import ValidationError

def test_config_requires_gitlab_webhook_token(monkeypatch):
    monkeypatch.delenv("GITLAB_WEBHOOK_TOKEN", raising=False)
    monkeypatch.delenv("GITLAB_API_TOKEN", raising=False)
    monkeypatch.delenv("ANTHROPIC_API_KEY", raising=False)
    from importlib import reload
    import ai_service.config as cfg_module
    with pytest.raises((ValidationError, Exception)):
        reload(cfg_module)
        cfg_module.Settings()

def test_config_defaults():
    import os
    os.environ.setdefault("GITLAB_WEBHOOK_TOKEN", "test-token")
    os.environ.setdefault("GITLAB_API_TOKEN", "test-api-token")
    os.environ.setdefault("GITLAB_URL", "https://gitlab.example.com")
    os.environ.setdefault("ANTHROPIC_API_KEY", "test-key")
    from ai_service.config import Settings
    s = Settings()
    assert s.anthropic_model == "claude-sonnet-4-6"
    assert s.sandbox_timeout == 60
    assert s.sandbox_memory_limit == "512m"
```

- [ ] **Step 2: 运行测试确认失败**

```bash
cd ai-service && uv run pytest tests/unit/test_config.py -v
```

Expected: FAIL — ModuleNotFoundError

- [ ] **Step 3: 实现 config.py**

```python
# src/ai_service/config.py
from pydantic_settings import BaseSettings

class Settings(BaseSettings):
    gitlab_webhook_token: str
    gitlab_api_token: str
    gitlab_url: str
    anthropic_api_key: str
    anthropic_model: str = "claude-sonnet-4-6"
    sandbox_timeout: int = 60
    sandbox_memory_limit: str = "512m"
    log_level: str = "INFO"

    class Config:
        env_file = ".env"

settings = Settings()
```

- [ ] **Step 4: 实现 logger.py**

```python
# src/ai_service/utils/logger.py
import logging
import sys

def get_logger(name: str) -> logging.Logger:
    logger = logging.getLogger(name)
    if not logger.handlers:
        handler = logging.StreamHandler(sys.stdout)
        handler.setFormatter(logging.Formatter(
            '{"time":"%(asctime)s","level":"%(levelname)s","name":"%(name)s","msg":"%(message)s"}'
        ))
        logger.addHandler(handler)
    return logger
```

- [ ] **Step 5: 创建 __init__.py 文件**

```bash
touch ai-service/src/ai_service/__init__.py
touch ai-service/src/ai_service/utils/__init__.py
touch ai-service/tests/__init__.py
touch ai-service/tests/unit/__init__.py
```

- [ ] **Step 6: 运行测试确认通过**

```bash
cd ai-service && uv run pytest tests/unit/test_config.py -v
```

Expected: PASS

- [ ] **Step 7: Commit**

```bash
git add ai-service/src/ ai-service/tests/
git commit -m "feat: add config and logger"
```

---

### Task 3: Flask 应用工厂 + 健康检查

**Files:**
- Create: `ai-service/src/ai_service/app.py`
- Create: `ai-service/src/ai_service/extensions.py`
- Create: `ai-service/src/ai_service/routes/__init__.py`
- Create: `ai-service/src/ai_service/routes/health.py`
- Create: `ai-service/tests/conftest.py`
- Create: `ai-service/tests/unit/test_health.py`

- [ ] **Step 1: 写失败测试**

```python
# tests/unit/test_health.py
def test_health_returns_ok(client):
    resp = client.get("/health")
    assert resp.status_code == 200
    data = resp.get_json()
    assert data["status"] == "ok"
    assert "version" in data
```

- [ ] **Step 2: 写 conftest.py**

```python
# tests/conftest.py
import pytest
import os
os.environ.setdefault("GITLAB_WEBHOOK_TOKEN", "test-token")
os.environ.setdefault("GITLAB_API_TOKEN", "test-api-token")
os.environ.setdefault("GITLAB_URL", "https://gitlab.example.com")
os.environ.setdefault("ANTHROPIC_API_KEY", "test-key")

from ai_service.app import create_app

@pytest.fixture
def app():
    app = create_app(testing=True)
    return app

@pytest.fixture
def client(app):
    return app.test_client()
```

- [ ] **Step 3: 运行测试确认失败**

```bash
cd ai-service && uv run pytest tests/unit/test_health.py -v
```

Expected: FAIL — ImportError

- [ ] **Step 4: 实现 health.py**

```python
# src/ai_service/routes/health.py
from flask import Blueprint, jsonify

bp = Blueprint("health", __name__)

@bp.get("/health")
def health():
    return jsonify({"status": "ok", "version": "1.0.0"})
```

- [ ] **Step 5: 实现 app.py**

```python
# src/ai_service/app.py
from flask import Flask
from ai_service.routes.health import bp as health_bp

def create_app(testing: bool = False) -> Flask:
    app = Flask(__name__)
    app.config["TESTING"] = testing
    app.register_blueprint(health_bp)
    return app
```

- [ ] **Step 6: 创建空 __init__.py**

```bash
touch ai-service/src/ai_service/routes/__init__.py
touch ai-service/src/ai_service/extensions.py
```

- [ ] **Step 7: 运行测试确认通过**

```bash
cd ai-service && uv run pytest tests/unit/test_health.py -v
```

Expected: PASS

- [ ] **Step 8: Commit**

```bash
git add ai-service/src/ai_service/app.py ai-service/src/ai_service/routes/ ai-service/src/ai_service/extensions.py ai-service/tests/conftest.py ai-service/tests/unit/test_health.py
git commit -m "feat: add Flask app factory and health endpoint"
```

---

### Task 4: GitLab Webhook 认证中间件

**Files:**
- Create: `ai-service/src/ai_service/middleware/__init__.py`
- Create: `ai-service/src/ai_service/middleware/gitlab_auth.py`
- Create: `ai-service/tests/unit/test_gitlab_auth.py`

- [ ] **Step 1: 写失败测试**

```python
# tests/unit/test_gitlab_auth.py
import pytest

def test_valid_token_passes(client):
    resp = client.post(
        "/webhook/gitlab",
        json={"object_kind": "merge_request"},
        headers={"X-Gitlab-Token": "test-token", "X-Gitlab-Event": "Merge Request Hook"},
    )
    assert resp.status_code != 401

def test_missing_token_returns_401(client):
    resp = client.post("/webhook/gitlab", json={})
    assert resp.status_code == 401

def test_wrong_token_returns_401(client):
    resp = client.post(
        "/webhook/gitlab",
        json={},
        headers={"X-Gitlab-Token": "wrong-token"},
    )
    assert resp.status_code == 401
```

- [ ] **Step 2: 运行测试确认失败**

```bash
cd ai-service && uv run pytest tests/unit/test_gitlab_auth.py -v
```

Expected: FAIL — 404 (路由不存在)

- [ ] **Step 3: 实现 gitlab_auth.py**

```python
# src/ai_service/middleware/gitlab_auth.py
from functools import wraps
from flask import request, jsonify
from ai_service.config import settings
from ai_service.utils.logger import get_logger

logger = get_logger(__name__)

def require_gitlab_token(f):
    @wraps(f)
    def decorated(*args, **kwargs):
        token = request.headers.get("X-Gitlab-Token", "")
        if token != settings.gitlab_webhook_token:
            logger.warning("gitlab_auth_failed remote_addr=%s", request.remote_addr)
            return jsonify({"error": "Unauthorized"}), 401
        return f(*args, **kwargs)
    return decorated
```

- [ ] **Step 4: 创建 webhook 路由（最小实现，供认证测试用）**

```python
# src/ai_service/routes/webhook.py
from flask import Blueprint, jsonify, request
from ai_service.middleware.gitlab_auth import require_gitlab_token

bp = Blueprint("webhook", __name__)

@bp.post("/webhook/gitlab")
@require_gitlab_token
def gitlab_webhook():
    return jsonify({"status": "ok"}), 200
```

- [ ] **Step 5: 注册 webhook 蓝图到 app.py**

```python
# src/ai_service/app.py
from flask import Flask
from ai_service.routes.health import bp as health_bp
from ai_service.routes.webhook import bp as webhook_bp

def create_app(testing: bool = False) -> Flask:
    app = Flask(__name__)
    app.config["TESTING"] = testing
    app.register_blueprint(health_bp)
    app.register_blueprint(webhook_bp)
    return app
```

- [ ] **Step 6: 运行测试确认通过**

```bash
cd ai-service && uv run pytest tests/unit/test_gitlab_auth.py -v
```

Expected: PASS

- [ ] **Step 7: Commit**

```bash
git add ai-service/src/ai_service/middleware/ ai-service/src/ai_service/routes/webhook.py ai-service/src/ai_service/app.py ai-service/tests/unit/test_gitlab_auth.py
git commit -m "feat: add GitLab webhook auth middleware"
```

---

### Task 5: 场景基类与分发器

**Files:**
- Create: `ai-service/src/ai_service/core/__init__.py`
- Create: `ai-service/src/ai_service/core/scenario.py`
- Create: `ai-service/src/ai_service/core/dispatcher.py`
- Create: `ai-service/tests/unit/test_dispatcher.py`

- [ ] **Step 1: 写失败测试**

```python
# tests/unit/test_dispatcher.py
from ai_service.core.dispatcher import Dispatcher
from ai_service.core.scenario import Scenario, ScenarioResult

class FakeScenario(Scenario):
    @property
    def name(self) -> str:
        return "fake"

    def can_handle(self, event_type: str, payload: dict) -> bool:
        return event_type == "Merge Request Hook"

    def handle(self, payload: dict) -> ScenarioResult:
        return ScenarioResult(success=True, message="handled")

def test_dispatcher_routes_to_matching_scenario():
    dispatcher = Dispatcher([FakeScenario()])
    result = dispatcher.dispatch("Merge Request Hook", {"object_kind": "merge_request"})
    assert result is not None
    assert result.success is True

def test_dispatcher_returns_none_for_unmatched_event():
    dispatcher = Dispatcher([FakeScenario()])
    result = dispatcher.dispatch("Push Hook", {})
    assert result is None
```

- [ ] **Step 2: 运行测试确认失败**

```bash
cd ai-service && uv run pytest tests/unit/test_dispatcher.py -v
```

Expected: FAIL — ImportError

- [ ] **Step 3: 实现 scenario.py**

```python
# src/ai_service/core/scenario.py
from abc import ABC, abstractmethod
from dataclasses import dataclass

@dataclass
class ScenarioResult:
    success: bool
    message: str

class Scenario(ABC):
    @property
    @abstractmethod
    def name(self) -> str: ...

    @abstractmethod
    def can_handle(self, event_type: str, payload: dict) -> bool: ...

    @abstractmethod
    def handle(self, payload: dict) -> ScenarioResult: ...
```

- [ ] **Step 4: 实现 dispatcher.py**

```python
# src/ai_service/core/dispatcher.py
from ai_service.core.scenario import Scenario, ScenarioResult
from ai_service.utils.logger import get_logger

logger = get_logger(__name__)

class Dispatcher:
    def __init__(self, scenarios: list[Scenario]):
        self._scenarios = scenarios

    def dispatch(self, event_type: str, payload: dict) -> ScenarioResult | None:
        for scenario in self._scenarios:
            if scenario.can_handle(event_type, payload):
                logger.info("dispatching event=%s to scenario=%s", event_type, scenario.name)
                return scenario.handle(payload)
        logger.info("no_scenario_matched event=%s", event_type)
        return None
```

- [ ] **Step 5: 运行测试确认通过**

```bash
cd ai-service && uv run pytest tests/unit/test_dispatcher.py -v
```

Expected: PASS

- [ ] **Step 6: Commit**

```bash
git add ai-service/src/ai_service/core/ ai-service/tests/unit/test_dispatcher.py
git commit -m "feat: add scenario base class and dispatcher"
```

---

### Task 6: Claude 服务封装

**Files:**
- Create: `ai-service/src/ai_service/services/__init__.py`
- Create: `ai-service/src/ai_service/services/claude.py`
- Create: `ai-service/tests/unit/test_claude_service.py`

- [ ] **Step 1: 写失败测试**

```python
# tests/unit/test_claude_service.py
from unittest.mock import MagicMock, patch
from ai_service.services.claude import ClaudeService

def test_call_returns_text_content():
    with patch("ai_service.services.claude.anthropic.Anthropic") as mock_cls:
        mock_client = MagicMock()
        mock_cls.return_value = mock_client
        mock_client.messages.create.return_value = MagicMock(
            content=[MagicMock(text="review result")],
            usage=MagicMock(input_tokens=100, output_tokens=50),
        )
        svc = ClaudeService()
        result = svc.call("You are helpful.", "Review this code.")
        assert result == "review result"

def test_call_retries_on_rate_limit():
    from anthropic import RateLimitError
    with patch("ai_service.services.claude.anthropic.Anthropic") as mock_cls:
        mock_client = MagicMock()
        mock_cls.return_value = mock_client
        mock_response = MagicMock(status_code=429, headers={})
        mock_client.messages.create.side_effect = [
            RateLimitError("rate limited", response=mock_response, body={}),
            MagicMock(
                content=[MagicMock(text="ok")],
                usage=MagicMock(input_tokens=10, output_tokens=5),
            ),
        ]
        svc = ClaudeService()
        result = svc.call("sys", "user", max_retries=3)
        assert result == "ok"
        assert mock_client.messages.create.call_count == 2
```

- [ ] **Step 2: 运行测试确认失败**

```bash
cd ai-service && uv run pytest tests/unit/test_claude_service.py -v
```

Expected: FAIL — ImportError

- [ ] **Step 3: 实现 claude.py**

```python
# src/ai_service/services/claude.py
import anthropic
from tenacity import retry, stop_after_attempt, wait_exponential, retry_if_exception_type
from ai_service.config import settings
from ai_service.utils.logger import get_logger

logger = get_logger(__name__)

class ClaudeService:
    def __init__(self):
        self._client = anthropic.Anthropic(api_key=settings.anthropic_api_key)

    def call(self, system: str, user: str, max_retries: int = 3) -> str:
        @retry(
            stop=stop_after_attempt(max_retries),
            wait=wait_exponential(multiplier=1, min=2, max=30),
            retry=retry_if_exception_type(anthropic.RateLimitError),
        )
        def _call():
            resp = self._client.messages.create(
                model=settings.anthropic_model,
                max_tokens=4096,
                system=system,
                messages=[{"role": "user", "content": user}],
            )
            logger.info("claude_call input=%d output=%d", resp.usage.input_tokens, resp.usage.output_tokens)
            return resp.content[0].text

        return _call()
```

- [ ] **Step 4: 运行测试确认通过**

```bash
cd ai-service && uv run pytest tests/unit/test_claude_service.py -v
```

Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add ai-service/src/ai_service/services/claude.py ai-service/src/ai_service/services/__init__.py ai-service/tests/unit/test_claude_service.py
git commit -m "feat: add Claude service with retry"
```

---

### Task 7: GitLab 服务封装

**Files:**
- Create: `ai-service/src/ai_service/services/gitlab.py`
- Create: `ai-service/tests/unit/test_gitlab_service.py`

- [ ] **Step 1: 写失败测试**

```python
# tests/unit/test_gitlab_service.py
from unittest.mock import patch, MagicMock
from ai_service.services.gitlab import GitLabService

def test_get_mr_diff_returns_diff():
    with patch("ai_service.services.gitlab.requests.get") as mock_get:
        mock_get.return_value = MagicMock(
            status_code=200,
            json=lambda: [{"diff": "- old\n+ new", "new_path": "main.py"}],
        )
        svc = GitLabService()
        diff = svc.get_mr_diff(project_id=1, mr_iid=2)
        assert "main.py" in diff

def test_post_mr_comment_calls_api():
    with patch("ai_service.services.gitlab.requests.post") as mock_post:
        mock_post.return_value = MagicMock(status_code=201)
        svc = GitLabService()
        svc.post_mr_comment(project_id=1, mr_iid=2, body="LGTM")
        mock_post.assert_called_once()
```

- [ ] **Step 2: 运行测试确认失败**

```bash
cd ai-service && uv run pytest tests/unit/test_gitlab_service.py -v
```

Expected: FAIL — ImportError

- [ ] **Step 3: 实现 gitlab.py**

```python
# src/ai_service/services/gitlab.py
import requests
from tenacity import retry, stop_after_attempt, wait_exponential
from ai_service.config import settings
from ai_service.utils.logger import get_logger

logger = get_logger(__name__)

class GitLabService:
    def __init__(self):
        self._base = settings.gitlab_url.rstrip("/") + "/api/v4"
        self._headers = {"PRIVATE-TOKEN": settings.gitlab_api_token}

    @retry(stop=stop_after_attempt(3), wait=wait_exponential(min=1, max=10))
    def get_mr_diff(self, project_id: int, mr_iid: int) -> str:
        url = f"{self._base}/projects/{project_id}/merge_requests/{mr_iid}/diffs"
        resp = requests.get(url, headers=self._headers, timeout=30)
        resp.raise_for_status()
        diffs = resp.json()
        return "\n".join(f"--- {d['new_path']}\n{d['diff']}" for d in diffs)

    @retry(stop=stop_after_attempt(3), wait=wait_exponential(min=1, max=10))
    def post_mr_comment(self, project_id: int, mr_iid: int, body: str) -> None:
        url = f"{self._base}/projects/{project_id}/merge_requests/{mr_iid}/notes"
        resp = requests.post(url, headers=self._headers, json={"body": body}, timeout=30)
        resp.raise_for_status()

    @retry(stop=stop_after_attempt(3), wait=wait_exponential(min=1, max=10))
    def post_issue_comment(self, project_id: int, issue_iid: int, body: str) -> None:
        url = f"{self._base}/projects/{project_id}/issues/{issue_iid}/notes"
        resp = requests.post(url, headers=self._headers, json={"body": body}, timeout=30)
        resp.raise_for_status()

    @retry(stop=stop_after_attempt(3), wait=wait_exponential(min=1, max=10))
    def create_issue(self, project_id: int, title: str, description: str) -> int:
        url = f"{self._base}/projects/{project_id}/issues"
        resp = requests.post(url, headers=self._headers, json={"title": title, "description": description}, timeout=30)
        resp.raise_for_status()
        return resp.json()["iid"]
```

- [ ] **Step 4: 运行测试确认通过**

```bash
cd ai-service && uv run pytest tests/unit/test_gitlab_service.py -v
```

Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add ai-service/src/ai_service/services/gitlab.py ai-service/tests/unit/test_gitlab_service.py
git commit -m "feat: add GitLab service with retry"
```

---

### Task 8: Docker 沙箱执行器

**Files:**
- Create: `ai-service/src/ai_service/services/sandbox.py`
- Create: `ai-service/sandbox/Dockerfile`
- Create: `ai-service/tests/unit/test_sandbox.py`

- [ ] **Step 1: 写失败测试**

```python
# tests/unit/test_sandbox.py
from unittest.mock import patch, MagicMock
from ai_service.services.sandbox import SandboxExecutor, SandboxResult

def test_execute_returns_result():
    with patch("ai_service.services.sandbox.docker.from_env") as mock_docker:
        mock_client = MagicMock()
        mock_docker.return_value = mock_client
        mock_client.containers.run.return_value = b"hello\n"
        executor = SandboxExecutor()
        result = executor.execute(code='print("hello")', language="python")
        assert isinstance(result, SandboxResult)
        assert result.success is True
        assert "hello" in result.output

def test_execute_captures_timeout():
    import docker.errors
    with patch("ai_service.services.sandbox.docker.from_env") as mock_docker:
        mock_client = MagicMock()
        mock_docker.return_value = mock_client
        mock_client.containers.run.side_effect = docker.errors.ContainerError(
            container=MagicMock(), exit_status=1, command="python", image="sandbox",
            stderr=b"Timeout"
        )
        executor = SandboxExecutor()
        result = executor.execute(code="import time; time.sleep(999)", language="python")
        assert result.success is False
```

- [ ] **Step 2: 运行测试确认失败**

```bash
cd ai-service && uv run pytest tests/unit/test_sandbox.py -v
```

Expected: FAIL — ImportError

- [ ] **Step 3: 实现 sandbox.py**

```python
# src/ai_service/services/sandbox.py
import docker
import docker.errors
from dataclasses import dataclass
from ai_service.config import settings
from ai_service.utils.logger import get_logger

logger = get_logger(__name__)

@dataclass
class SandboxResult:
    success: bool
    output: str

class SandboxExecutor:
    IMAGES = {"python": "python:3.12-slim", "bash": "bash:5"}

    def __init__(self):
        self._client = docker.from_env()

    def execute(
        self,
        code: str,
        language: str,
        timeout: int | None = None,
        memory_limit: str | None = None,
    ) -> SandboxResult:
        image = self.IMAGES.get(language, "python:3.12-slim")
        cmd = ["python", "-c", code] if language == "python" else ["bash", "-c", code]
        try:
            output = self._client.containers.run(
                image=image,
                command=cmd,
                mem_limit=memory_limit or settings.sandbox_memory_limit,
                network_disabled=True,
                remove=True,
                stdout=True,
                stderr=True,
                timeout=timeout or settings.sandbox_timeout,
            )
            return SandboxResult(success=True, output=output.decode())
        except docker.errors.ContainerError as e:
            return SandboxResult(success=False, output=e.stderr.decode() if e.stderr else str(e))
        except Exception as e:
            logger.error("sandbox_error %s", e)
            return SandboxResult(success=False, output=str(e))
```

- [ ] **Step 4: 创建沙箱 Dockerfile**

```dockerfile
# sandbox/Dockerfile
FROM python:3.12-slim
RUN pip install --no-cache-dir ruff mypy
WORKDIR /workspace
```

- [ ] **Step 5: 运行测试确认通过**

```bash
cd ai-service && uv run pytest tests/unit/test_sandbox.py -v
```

Expected: PASS

- [ ] **Step 6: Commit**

```bash
git add ai-service/src/ai_service/services/sandbox.py ai-service/sandbox/ ai-service/tests/unit/test_sandbox.py
git commit -m "feat: add Docker sandbox executor"
```

---

### Task 9: 代码评审场景

**Files:**
- Create: `ai-service/src/ai_service/scenarios/__init__.py`
- Create: `ai-service/src/ai_service/scenarios/code_review.py`
- Create: `ai-service/tests/unit/test_scenarios/__init__.py`
- Create: `ai-service/tests/unit/test_scenarios/test_code_review.py`

- [ ] **Step 1: 写失败测试**

```python
# tests/unit/test_scenarios/test_code_review.py
from unittest.mock import MagicMock
from ai_service.scenarios.code_review import CodeReviewScenario

def _make_scenario(diff="- old\n+ new", review="LGTM"):
    gitlab = MagicMock()
    gitlab.get_mr_diff.return_value = diff
    claude = MagicMock()
    claude.call.return_value = review
    sandbox = MagicMock()
    sandbox.execute.return_value = MagicMock(success=True, output="No issues")
    return CodeReviewScenario(gitlab_service=gitlab, claude_service=claude, sandbox=sandbox)

def test_can_handle_mr_open():
    s = _make_scenario()
    assert s.can_handle("Merge Request Hook", {"object_attributes": {"action": "open"}})

def test_can_handle_mr_update():
    s = _make_scenario()
    assert s.can_handle("Merge Request Hook", {"object_attributes": {"action": "update"}})

def test_cannot_handle_other_events():
    s = _make_scenario()
    assert not s.can_handle("Push Hook", {})

def test_handle_posts_comment():
    s = _make_scenario()
    payload = {
        "project": {"id": 1},
        "object_attributes": {"iid": 2, "action": "open"},
    }
    result = s.handle(payload)
    assert result.success is True
    s._gitlab.post_mr_comment.assert_called_once()
```

- [ ] **Step 2: 运行测试确认失败**

```bash
cd ai-service && uv run pytest tests/unit/test_scenarios/test_code_review.py -v
```

Expected: FAIL — ImportError

- [ ] **Step 3: 实现 code_review.py**

```python
# src/ai_service/scenarios/code_review.py
from ai_service.core.scenario import Scenario, ScenarioResult
from ai_service.services.claude import ClaudeService
from ai_service.services.gitlab import GitLabService
from ai_service.services.sandbox import SandboxExecutor

SYSTEM_PROMPT = "你是一位资深代码审查员。"
USER_TEMPLATE = """请对以下代码变更进行评审：

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

如有修复建议，请提供具体代码示例。"""

class CodeReviewScenario(Scenario):
    def __init__(self, gitlab_service: GitLabService, claude_service: ClaudeService, sandbox: SandboxExecutor):
        self._gitlab = gitlab_service
        self._claude = claude_service
        self._sandbox = sandbox

    @property
    def name(self) -> str:
        return "code_review"

    def can_handle(self, event_type: str, payload: dict) -> bool:
        if event_type != "Merge Request Hook":
            return False
        action = payload.get("object_attributes", {}).get("action", "")
        return action in ("open", "update")

    def handle(self, payload: dict) -> ScenarioResult:
        project_id = payload["project"]["id"]
        mr_iid = payload["object_attributes"]["iid"]
        diff = self._gitlab.get_mr_diff(project_id, mr_iid)
        review = self._claude.call(SYSTEM_PROMPT, USER_TEMPLATE.format(diff=diff))
        sandbox_result = self._sandbox.execute(code=f"# review\n{review}", language="python")
        comment = f"{review}\n\n---\n**沙箱验证：** {'通过' if sandbox_result.success else '失败'}\n```\n{sandbox_result.output}\n```"
        self._gitlab.post_mr_comment(project_id, mr_iid, comment)
        return ScenarioResult(success=True, message="code review posted")
```

- [ ] **Step 4: 运行测试确认通过**

```bash
cd ai-service && uv run pytest tests/unit/test_scenarios/test_code_review.py -v
```

Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add ai-service/src/ai_service/scenarios/ ai-service/tests/unit/test_scenarios/
git commit -m "feat: add code review scenario"
```

---

### Task 10: 需求拆分场景

**Files:**
- Create: `ai-service/src/ai_service/scenarios/story_split.py`
- Create: `ai-service/tests/unit/test_scenarios/test_story_split.py`

- [ ] **Step 1: 写失败测试**

```python
# tests/unit/test_scenarios/test_story_split.py
import json
from unittest.mock import MagicMock
from ai_service.scenarios.story_split import StorySplitScenario

def _make_scenario(claude_output=None):
    if claude_output is None:
        claude_output = json.dumps([
            {"title": "子任务1", "description": "desc", "acceptance_criteria": ["ac1"], "estimated_hours": 4}
        ])
    gitlab = MagicMock()
    claude = MagicMock()
    claude.call.return_value = claude_output
    sandbox = MagicMock()
    sandbox.execute.return_value = MagicMock(success=True, output="valid")
    return StorySplitScenario(gitlab_service=gitlab, claude_service=claude, sandbox=sandbox)

def test_can_handle_issue_with_needs_split_label():
    s = _make_scenario()
    payload = {"object_attributes": {"action": "open"}, "labels": [{"title": "needs-split"}]}
    assert s.can_handle("Issue Hook", payload)

def test_cannot_handle_issue_without_label():
    s = _make_scenario()
    payload = {"object_attributes": {"action": "open"}, "labels": []}
    assert not s.can_handle("Issue Hook", payload)

def test_handle_creates_sub_issues():
    s = _make_scenario()
    payload = {
        "project": {"id": 1},
        "object_attributes": {"iid": 3, "action": "open", "title": "大需求", "description": "详细描述"},
        "labels": [{"title": "needs-split"}],
    }
    result = s.handle(payload)
    assert result.success is True
    s._gitlab.create_issue.assert_called_once()
```

- [ ] **Step 2: 运行测试确认失败**

```bash
cd ai-service && uv run pytest tests/unit/test_scenarios/test_story_split.py -v
```

Expected: FAIL — ImportError

- [ ] **Step 3: 实现 story_split.py**

```python
# src/ai_service/scenarios/story_split.py
import json
from ai_service.core.scenario import Scenario, ScenarioResult
from ai_service.services.claude import ClaudeService
from ai_service.services.gitlab import GitLabService
from ai_service.services.sandbox import SandboxExecutor

SYSTEM_PROMPT = "你是一位资深产品经理。"
USER_TEMPLATE = """请将以下需求拆分为可独立开发、测试、交付的子任务：

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
- estimated_hours: 预估工时"""

class StorySplitScenario(Scenario):
    def __init__(self, gitlab_service: GitLabService, claude_service: ClaudeService, sandbox: SandboxExecutor):
        self._gitlab = gitlab_service
        self._claude = claude_service
        self._sandbox = sandbox

    @property
    def name(self) -> str:
        return "story_split"

    def can_handle(self, event_type: str, payload: dict) -> bool:
        if event_type != "Issue Hook":
            return False
        if payload.get("object_attributes", {}).get("action") != "open":
            return False
        labels = [l["title"] for l in payload.get("labels", [])]
        return "needs-split" in labels

    def handle(self, payload: dict) -> ScenarioResult:
        project_id = payload["project"]["id"]
        issue_iid = payload["object_attributes"]["iid"]
        title = payload["object_attributes"]["title"]
        description = payload["object_attributes"].get("description", "")
        raw = self._claude.call(SYSTEM_PROMPT, USER_TEMPLATE.format(title=title, description=description))
        validate_code = f"import json; data = json.loads({repr(raw)}); assert isinstance(data, list)"
        self._sandbox.execute(code=validate_code, language="python")
        try:
            subtasks = json.loads(raw)
        except json.JSONDecodeError:
            return ScenarioResult(success=False, message="invalid JSON from Claude")
        for task in subtasks:
            ac = "\n".join(f"- {c}" for c in task.get("acceptance_criteria", []))
            desc = f"{task['description']}\n\n**验收标准：**\n{ac}\n\n**预估工时：** {task.get('estimated_hours', '?')}h"
            self._gitlab.create_issue(project_id, task["title"], desc)
        self._gitlab.post_issue_comment(project_id, issue_iid, f"已拆分为 {len(subtasks)} 个子任务。")
        return ScenarioResult(success=True, message=f"created {len(subtasks)} sub-issues")
```

- [ ] **Step 4: 运行测试确认通过**

```bash
cd ai-service && uv run pytest tests/unit/test_scenarios/test_story_split.py -v
```

Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add ai-service/src/ai_service/scenarios/story_split.py ai-service/tests/unit/test_scenarios/test_story_split.py
git commit -m "feat: add story split scenario"
```

---

### Task 11: Bugfix 场景

**Files:**
- Create: `ai-service/src/ai_service/scenarios/bugfix.py`
- Create: `ai-service/tests/unit/test_scenarios/test_bugfix.py`

- [ ] **Step 1: 写失败测试**

```python
# tests/unit/test_scenarios/test_bugfix.py
from unittest.mock import MagicMock
from ai_service.scenarios.bugfix import BugfixScenario

def _make_scenario(review="根因：...\n修复代码：```python\npass\n```"):
    gitlab = MagicMock()
    claude = MagicMock()
    claude.call.return_value = review
    sandbox = MagicMock()
    sandbox.execute.return_value = MagicMock(success=True, output="")
    return BugfixScenario(gitlab_service=gitlab, claude_service=claude, sandbox=sandbox)

def test_can_handle_bug_issue():
    s = _make_scenario()
    payload = {"object_attributes": {"action": "open"}, "labels": [{"title": "bug"}]}
    assert s.can_handle("Issue Hook", payload)

def test_cannot_handle_without_bug_label():
    s = _make_scenario()
    payload = {"object_attributes": {"action": "open"}, "labels": []}
    assert not s.can_handle("Issue Hook", payload)

def test_handle_posts_analysis():
    s = _make_scenario()
    payload = {
        "project": {"id": 1},
        "object_attributes": {"iid": 5, "action": "open", "title": "NPE in login", "description": "crash on null"},
        "labels": [{"title": "bug"}],
    }
    result = s.handle(payload)
    assert result.success is True
    s._gitlab.post_issue_comment.assert_called_once()
```

- [ ] **Step 2: 运行测试确认失败**

```bash
cd ai-service && uv run pytest tests/unit/test_scenarios/test_bugfix.py -v
```

Expected: FAIL — ImportError

- [ ] **Step 3: 实现 bugfix.py**

```python
# src/ai_service/scenarios/bugfix.py
import re
from ai_service.core.scenario import Scenario, ScenarioResult
from ai_service.services.claude import ClaudeService
from ai_service.services.gitlab import GitLabService
from ai_service.services.sandbox import SandboxExecutor

SYSTEM_PROMPT = "你是一位资深开发工程师。"
USER_TEMPLATE = """请分析以下 Bug 并提供修复建议：

【Bug 描述】
标题：{title}
描述：{description}

【分析要求】
1. 根因分析
2. 修复方案（具体代码修改）
3. 预防措施
4. 测试建议

【输出格式】
- 根因：...
- 修复代码：```language\n...\n```
- 预防措施：...
- 测试建议：..."""

class BugfixScenario(Scenario):
    def __init__(self, gitlab_service: GitLabService, claude_service: ClaudeService, sandbox: SandboxExecutor):
        self._gitlab = gitlab_service
        self._claude = claude_service
        self._sandbox = sandbox

    @property
    def name(self) -> str:
        return "bugfix"

    def can_handle(self, event_type: str, payload: dict) -> bool:
        if event_type != "Issue Hook":
            return False
        if payload.get("object_attributes", {}).get("action") != "open":
            return False
        labels = [l["title"] for l in payload.get("labels", [])]
        return "bug" in labels

    def handle(self, payload: dict) -> ScenarioResult:
        project_id = payload["project"]["id"]
        issue_iid = payload["object_attributes"]["iid"]
        title = payload["object_attributes"]["title"]
        description = payload["object_attributes"].get("description", "")
        analysis = self._claude.call(SYSTEM_PROMPT, USER_TEMPLATE.format(title=title, description=description))
        sandbox_output = ""
        match = re.search(r"```python\n(.*?)```", analysis, re.DOTALL)
        if match:
            result = self._sandbox.execute(code=match.group(1), language="python")
            sandbox_output = f"\n\n---\n**沙箱验证：** {'通过' if result.success else '失败'}\n```\n{result.output}\n```"
        self._gitlab.post_issue_comment(project_id, issue_iid, analysis + sandbox_output)
        return ScenarioResult(success=True, message="bugfix analysis posted")
```

- [ ] **Step 4: 运行测试确认通过**

```bash
cd ai-service && uv run pytest tests/unit/test_scenarios/test_bugfix.py -v
```

Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add ai-service/src/ai_service/scenarios/bugfix.py ai-service/tests/unit/test_scenarios/test_bugfix.py
git commit -m "feat: add bugfix scenario"
```

---

### Task 12: 集成 Webhook 路由与场景注册

**Files:**
- Modify: `ai-service/src/ai_service/routes/webhook.py`
- Create: `ai-service/tests/integration/test_webhook_flow.py`

- [ ] **Step 1: 写集成测试**

```python
# tests/integration/test_webhook_flow.py
from unittest.mock import patch, MagicMock

def test_mr_webhook_triggers_code_review(client):
    with patch("ai_service.routes.webhook.dispatcher") as mock_dispatcher:
        mock_dispatcher.dispatch.return_value = MagicMock(success=True, message="ok")
        resp = client.post(
            "/webhook/gitlab",
            json={"object_kind": "merge_request", "object_attributes": {"action": "open"}, "project": {"id": 1}},
            headers={"X-Gitlab-Token": "test-token", "X-Gitlab-Event": "Merge Request Hook"},
        )
        assert resp.status_code == 200
        mock_dispatcher.dispatch.assert_called_once_with(
            "Merge Request Hook",
            {"object_kind": "merge_request", "object_attributes": {"action": "open"}, "project": {"id": 1}},
        )

def test_unmatched_event_returns_200(client):
    with patch("ai_service.routes.webhook.dispatcher") as mock_dispatcher:
        mock_dispatcher.dispatch.return_value = None
        resp = client.post(
            "/webhook/gitlab",
            json={},
            headers={"X-Gitlab-Token": "test-token", "X-Gitlab-Event": "Push Hook"},
        )
        assert resp.status_code == 200
```

- [ ] **Step 2: 运行测试确认失败**

```bash
cd ai-service && uv run pytest tests/integration/test_webhook_flow.py -v
```

Expected: FAIL

- [ ] **Step 3: 更新 webhook.py 集成 dispatcher**

```python
# src/ai_service/routes/webhook.py
from flask import Blueprint, jsonify, request
from ai_service.middleware.gitlab_auth import require_gitlab_token
from ai_service.core.dispatcher import Dispatcher
from ai_service.scenarios.code_review import CodeReviewScenario
from ai_service.scenarios.story_split import StorySplitScenario
from ai_service.scenarios.bugfix import BugfixScenario
from ai_service.services.claude import ClaudeService
from ai_service.services.gitlab import GitLabService
from ai_service.services.sandbox import SandboxExecutor

bp = Blueprint("webhook", __name__)

def _build_dispatcher() -> Dispatcher:
    gitlab = GitLabService()
    claude = ClaudeService()
    sandbox = SandboxExecutor()
    return Dispatcher([
        CodeReviewScenario(gitlab, claude, sandbox),
        StorySplitScenario(gitlab, claude, sandbox),
        BugfixScenario(gitlab, claude, sandbox),
    ])

dispatcher = _build_dispatcher()

@bp.post("/webhook/gitlab")
@require_gitlab_token
def gitlab_webhook():
    event_type = request.headers.get("X-Gitlab-Event", "")
    payload = request.get_json(force=True) or {}
    dispatcher.dispatch(event_type, payload)
    return jsonify({"status": "ok"}), 200
```

- [ ] **Step 4: 运行所有测试**

```bash
cd ai-service && uv run pytest tests/ -v
```

Expected: 全部 PASS

- [ ] **Step 5: Commit**

```bash
git add ai-service/src/ai_service/routes/webhook.py ai-service/tests/integration/
git commit -m "feat: wire dispatcher into webhook route"
```

---

### Task 13: Prometheus Metrics + Gunicorn 配置

**Files:**
- Create: `ai-service/src/ai_service/routes/metrics.py`
- Create: `ai-service/gunicorn.conf.py`
- Modify: `ai-service/src/ai_service/app.py`

- [ ] **Step 1: 实现 metrics 路由**

```python
# src/ai_service/routes/metrics.py
from flask import Blueprint
from prometheus_flask_exporter import PrometheusMetrics

bp = Blueprint("metrics", __name__)
metrics: PrometheusMetrics | None = None

def init_metrics(app):
    global metrics
    metrics = PrometheusMetrics(app, path="/metrics")
    metrics.info("app_info", "AI Service", version="1.0.0")
```

- [ ] **Step 2: 更新 app.py 注册 metrics**

```python
# src/ai_service/app.py
from flask import Flask
from ai_service.routes.health import bp as health_bp
from ai_service.routes.webhook import bp as webhook_bp
from ai_service.routes.metrics import init_metrics

def create_app(testing: bool = False) -> Flask:
    app = Flask(__name__)
    app.config["TESTING"] = testing
    app.register_blueprint(health_bp)
    app.register_blueprint(webhook_bp)
    if not testing:
        init_metrics(app)
    return app
```

- [ ] **Step 3: 写 gunicorn.conf.py**

```python
# gunicorn.conf.py
import multiprocessing
bind = "0.0.0.0:5000"
workers = multiprocessing.cpu_count() * 2 + 1
worker_class = "gevent"
timeout = 120
accesslog = "-"
errorlog = "-"
```

- [ ] **Step 4: 运行测试确认无回归**

```bash
cd ai-service && uv run pytest tests/ -v
```

Expected: 全部 PASS

- [ ] **Step 5: Commit**

```bash
git add ai-service/src/ai_service/routes/metrics.py ai-service/src/ai_service/app.py ai-service/gunicorn.conf.py
git commit -m "feat: add Prometheus metrics and Gunicorn config"
```

---

### Task 14: Dockerfile + docker-compose

**Files:**
- Create: `ai-service/Dockerfile`
- Create: `ai-service/docker-compose.yml`

- [ ] **Step 1: 写 Dockerfile**

```dockerfile
# ai-service/Dockerfile
FROM python:3.12-slim
WORKDIR /app
COPY pyproject.toml uv.lock ./
RUN pip install uv && uv sync --no-dev
COPY src/ ./src/
EXPOSE 5000
CMD ["uv", "run", "gunicorn", "-c", "gunicorn.conf.py", "ai_service.app:create_app()"]
```

- [ ] **Step 2: 写 docker-compose.yml**

```yaml
# ai-service/docker-compose.yml
services:
  ai-service:
    build: .
    ports:
      - "5000:5000"
    volumes:
      - /var/run/docker.sock:/var/run/docker.sock
    env_file: .env
    restart: unless-stopped
```

- [ ] **Step 3: 构建镜像验证**

```bash
cd ai-service && docker build -t ai-service:dev .
```

Expected: 构建成功，无错误

- [ ] **Step 4: Commit**

```bash
git add ai-service/Dockerfile ai-service/docker-compose.yml
git commit -m "feat: add Dockerfile and docker-compose"
```

---

### Task 15: 运行全量测试并验收

- [ ] **Step 1: 运行全量单元测试**

```bash
cd ai-service && uv run pytest tests/unit/ -v --tb=short
```

Expected: 全部 PASS

- [ ] **Step 2: 运行集成测试**

```bash
cd ai-service && uv run pytest tests/integration/ -v --tb=short
```

Expected: 全部 PASS

- [ ] **Step 3: 验证健康检查端点**

```bash
cd ai-service && uv run flask --app "ai_service.app:create_app()" run --port 5001 &
sleep 2 && curl -s http://localhost:5001/health
```

Expected: `{"status":"ok","version":"1.0.0"}`

- [ ] **Step 4: 停止测试服务器并最终 commit**

```bash
kill %1
git add -A
git commit -m "feat: complete ai-service iteration 1"
```
