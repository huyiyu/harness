from unittest.mock import patch, MagicMock
from ai_service.services.gitlab import GitLabService

def test_get_mr_diff_returns_diff():
    with patch("ai_service.services.gitlab.requests.get") as mock_get:
        mock_get.return_value = MagicMock(
            status_code=200,
            json=lambda: [{"diff": "- old\n+ new", "new_path": "main.py"}],
        )
        svc = GitLabService()
        diff = svc.get_mr_diff(project_id=1, mr_iid=2)
        assert "main.py" in diff

def test_post_mr_comment_calls_api():
    with patch("ai_service.services.gitlab.requests.post") as mock_post:
        mock_post.return_value = MagicMock(status_code=201, raise_for_status=lambda: None)
        svc = GitLabService()
        svc.post_mr_comment(project_id=1, mr_iid=2, body="LGTM")
        mock_post.assert_called_once()
