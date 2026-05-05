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
