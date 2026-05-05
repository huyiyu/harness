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
