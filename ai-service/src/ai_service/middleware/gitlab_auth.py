from functools import wraps
from flask import request, jsonify
from ai_service.config import settings
from ai_service.utils.logger import get_logger

logger = get_logger(__name__)

def require_gitlab_token(f):
    @wraps(f)
    def decorated(*args, **kwargs):
        token = request.headers.get("X-Gitlab-Token", "")
        if token != settings.gitlab_webhook_token:
            logger.warning("gitlab_auth_failed remote_addr=%s", request.remote_addr)
            return jsonify({"error": "Unauthorized"}), 401
        return f(*args, **kwargs)
    return decorated
