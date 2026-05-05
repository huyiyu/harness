from flask import Blueprint, jsonify, request
from ai_service.middleware.gitlab_auth import require_gitlab_token
from ai_service.core.dispatcher import Dispatcher
from ai_service.scenarios.code_review import CodeReviewScenario
from ai_service.scenarios.story_split import StorySplitScenario
from ai_service.scenarios.bugfix import BugfixScenario
from ai_service.services.claude import ClaudeService
from ai_service.services.gitlab import GitLabService
from ai_service.services.sandbox import SandboxExecutor

bp = Blueprint("webhook", __name__)

def _build_dispatcher() -> Dispatcher:
    gitlab = GitLabService()
    claude = ClaudeService()
    sandbox = SandboxExecutor()
    return Dispatcher([
        CodeReviewScenario(gitlab, claude, sandbox),
        StorySplitScenario(gitlab, claude, sandbox),
        BugfixScenario(gitlab, claude, sandbox),
    ])

dispatcher = _build_dispatcher()

@bp.post("/webhook/gitlab")
@require_gitlab_token
def gitlab_webhook():
    event_type = request.headers.get("X-Gitlab-Event", "")
    payload = request.get_json(force=True) or {}
    dispatcher.dispatch(event_type, payload)
    return jsonify({"status": "ok"}), 200
