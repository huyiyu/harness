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
