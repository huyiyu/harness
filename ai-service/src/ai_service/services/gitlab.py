import requests
from tenacity import retry, stop_after_attempt, wait_exponential
from ai_service.config import settings
from ai_service.utils.logger import get_logger

logger = get_logger(__name__)

class GitLabService:
    def __init__(self):
        self._base = settings.gitlab_url.rstrip("/") + "/api/v4"
        self._headers = {"PRIVATE-TOKEN": settings.gitlab_api_token}

    @retry(stop=stop_after_attempt(3), wait=wait_exponential(min=1, max=10))
    def get_mr_diff(self, project_id: int, mr_iid: int) -> str:
        url = f"{self._base}/projects/{project_id}/merge_requests/{mr_iid}/diffs"
        resp = requests.get(url, headers=self._headers, timeout=30)
        resp.raise_for_status()
        diffs = resp.json()
        return "\n".join(f"--- {d['new_path']}\n{d['diff']}" for d in diffs)

    @retry(stop=stop_after_attempt(3), wait=wait_exponential(min=1, max=10))
    def post_mr_comment(self, project_id: int, mr_iid: int, body: str) -> None:
        url = f"{self._base}/projects/{project_id}/merge_requests/{mr_iid}/notes"
        resp = requests.post(url, headers=self._headers, json={"body": body}, timeout=30)
        resp.raise_for_status()

    @retry(stop=stop_after_attempt(3), wait=wait_exponential(min=1, max=10))
    def post_issue_comment(self, project_id: int, issue_iid: int, body: str) -> None:
        url = f"{self._base}/projects/{project_id}/issues/{issue_iid}/notes"
        resp = requests.post(url, headers=self._headers, json={"body": body}, timeout=30)
        resp.raise_for_status()

    @retry(stop=stop_after_attempt(3), wait=wait_exponential(min=1, max=10))
    def create_issue(self, project_id: int, title: str, description: str) -> int:
        url = f"{self._base}/projects/{project_id}/issues"
        resp = requests.post(url, headers=self._headers, json={"title": title, "description": description}, timeout=30)
        resp.raise_for_status()
        return resp.json()["iid"]
