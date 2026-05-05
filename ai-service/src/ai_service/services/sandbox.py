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
