import pytest
import os
os.environ.setdefault("GITLAB_WEBHOOK_TOKEN", "test-token")
os.environ.setdefault("GITLAB_API_TOKEN", "test-api-token")
os.environ.setdefault("GITLAB_URL", "https://gitlab.example.com")
os.environ.setdefault("ANTHROPIC_API_KEY", "test-key")

from ai_service.app import create_app

@pytest.fixture
def app():
    return create_app(testing=True)

@pytest.fixture
def client(app):
    return app.test_client()
