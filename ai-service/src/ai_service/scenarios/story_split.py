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
