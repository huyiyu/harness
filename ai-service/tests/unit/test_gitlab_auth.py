def test_valid_token_passes(client):
    resp = client.post(
        "/webhook/gitlab",
        json={"object_kind": "merge_request"},
        headers={"X-Gitlab-Token": "test-token", "X-Gitlab-Event": "Merge Request Hook"},
    )
    assert resp.status_code != 401

def test_missing_token_returns_401(client):
    resp = client.post("/webhook/gitlab", json={})
    assert resp.status_code == 401

def test_wrong_token_returns_401(client):
    resp = client.post(
        "/webhook/gitlab",
        json={},
        headers={"X-Gitlab-Token": "wrong-token"},
    )
    assert resp.status_code == 401
