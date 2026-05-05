from unittest.mock import MagicMock
from ai_service.scenarios.bugfix import BugfixScenario

def _make_scenario(review="根因：...\n修复代码：```python\npass\n```"):
    gitlab = MagicMock()
    claude = MagicMock()
    claude.call.return_value = review
    sandbox = MagicMock()
    sandbox.execute.return_value = MagicMock(success=True, output="")
    return BugfixScenario(gitlab_service=gitlab, claude_service=claude, sandbox=sandbox)

def test_can_handle_bug_issue():
    s = _make_scenario()
    payload = {"object_attributes": {"action": "open"}, "labels": [{"title": "bug"}]}
    assert s.can_handle("Issue Hook", payload)

def test_cannot_handle_without_bug_label():
    s = _make_scenario()
    payload = {"object_attributes": {"action": "open"}, "labels": []}
    assert not s.can_handle("Issue Hook", payload)

def test_handle_posts_analysis():
    s = _make_scenario()
    payload = {
        "project": {"id": 1},
        "object_attributes": {"iid": 5, "action": "open", "title": "NPE in login", "description": "crash on null"},
        "labels": [{"title": "bug"}],
    }
    result = s.handle(payload)
    assert result.success is True
    s._gitlab.post_issue_comment.assert_called_once()
