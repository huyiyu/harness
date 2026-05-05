import pytest
from pydantic import ValidationError

def test_config_requires_gitlab_webhook_token(monkeypatch):
    monkeypatch.delenv("GITLAB_WEBHOOK_TOKEN", raising=False)
    monkeypatch.delenv("GITLAB_API_TOKEN", raising=False)
    monkeypatch.delenv("ANTHROPIC_API_KEY", raising=False)
    monkeypatch.delenv("GITLAB_URL", raising=False)
    with pytest.raises((ValidationError, Exception)):
        from importlib import import_module, reload
        import sys
        sys.modules.pop("ai_service.config", None)
        import_module("ai_service.config")

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
