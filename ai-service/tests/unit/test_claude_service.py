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
