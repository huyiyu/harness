import json
from unittest.mock import MagicMock
from ai_service.scenarios.story_split import StorySplitScenario

def _make_scenario(claude_output=None):
    if claude_output is None:
        claude_output = json.dumps([
            {"title": "子任务1", "description": "desc", "acceptance_criteria": ["ac1"], "estimated_hours": 4}
        ])
    gitlab = MagicMock()
    claude = MagicMock()
    claude.call.return_value = claude_output
    sandbox = MagicMock()
    sandbox.execute.return_value = MagicMock(success=True, output="valid")
    return StorySplitScenario(gitlab_service=gitlab, claude_service=claude, sandbox=sandbox)

def test_can_handle_issue_with_needs_split_label():
    s = _make_scenario()
    payload = {"object_attributes": {"action": "open"}, "labels": [{"title": "needs-split"}]}
    assert s.can_handle("Issue Hook", payload)

def test_cannot_handle_issue_without_label():
    s = _make_scenario()
    payload = {"object_attributes": {"action": "open"}, "labels": []}
    assert not s.can_handle("Issue Hook", payload)

def test_handle_creates_sub_issues():
    s = _make_scenario()
    payload = {
        "project": {"id": 1},
        "object_attributes": {"iid": 3, "action": "open", "title": "大需求", "description": "详细描述"},
        "labels": [{"title": "needs-split"}],
    }
    result = s.handle(payload)
    assert result.success is True
    s._gitlab.create_issue.assert_called_once()
