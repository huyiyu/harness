from flask import Blueprint, jsonify
from ai_service.middleware.gitlab_auth import require_gitlab_token

bp = Blueprint("webhook", __name__)

@bp.post("/webhook/gitlab")
@require_gitlab_token
def gitlab_webhook():
    return jsonify({"status": "ok"}), 200
