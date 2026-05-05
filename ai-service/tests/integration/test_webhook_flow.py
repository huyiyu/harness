from unittest.mock import patch, MagicMock

def test_mr_webhook_triggers_code_review(client):
    with patch("ai_service.routes.webhook.dispatcher") as mock_dispatcher:
        mock_dispatcher.dispatch.return_value = MagicMock(success=True, message="ok")
        resp = client.post(
            "/webhook/gitlab",
            json={"object_kind": "merge_request", "object_attributes": {"action": "open"}, "project": {"id": 1}},
            headers={"X-Gitlab-Token": "test-token", "X-Gitlab-Event": "Merge Request Hook"},
        )
        assert resp.status_code == 200
        mock_dispatcher.dispatch.assert_called_once_with(
            "Merge Request Hook",
            {"object_kind": "merge_request", "object_attributes": {"action": "open"}, "project": {"id": 1}},
        )

def test_unmatched_event_returns_200(client):
    with patch("ai_service.routes.webhook.dispatcher") as mock_dispatcher:
        mock_dispatcher.dispatch.return_value = None
        resp = client.post(
            "/webhook/gitlab",
            json={},
            headers={"X-Gitlab-Token": "test-token", "X-Gitlab-Event": "Push Hook"},
        )
        assert resp.status_code == 200
