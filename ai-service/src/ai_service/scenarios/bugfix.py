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
