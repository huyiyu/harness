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
