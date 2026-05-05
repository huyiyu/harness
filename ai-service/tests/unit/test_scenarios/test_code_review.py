from unittest.mock import MagicMock
from ai_service.scenarios.code_review import CodeReviewScenario

def _make_scenario(diff="- old\n+ new", review="LGTM"):
    gitlab = MagicMock()
    gitlab.get_mr_diff.return_value = diff
    claude = MagicMock()
    claude.call.return_value = review
    sandbox = MagicMock()
    sandbox.execute.return_value = MagicMock(success=True, output="No issues")
    return CodeReviewScenario(gitlab_service=gitlab, claude_service=claude, sandbox=sandbox)

def test_can_handle_mr_open():
    s = _make_scenario()
    assert s.can_handle("Merge Request Hook", {"object_attributes": {"action": "open"}})

def test_can_handle_mr_update():
    s = _make_scenario()
    assert s.can_handle("Merge Request Hook", {"object_attributes": {"action": "update"}})

def test_cannot_handle_other_events():
    s = _make_scenario()
    assert not s.can_handle("Push Hook", {})

def test_handle_posts_comment():
    s = _make_scenario()
    payload = {
        "project": {"id": 1},
        "object_attributes": {"iid": 2, "action": "open"},
    }
    result = s.handle(payload)
    assert result.success is True
    s._gitlab.post_mr_comment.assert_called_once()
