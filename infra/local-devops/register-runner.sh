#!/bin/bash
set -e

source "$(dirname "$0")/.env"

GITLAB_URL="http://localhost:${GITLAB_PORT}"
# Runner 容器内通过 docker network 访问 GitLab，用服务名而非 localhost
GITLAB_INTERNAL_URL="http://gitlab:${GITLAB_PORT}"

echo "Waiting for GitLab to be ready..."
until curl -sf "${GITLAB_URL}/-/health" > /dev/null; do
  sleep 5
done
echo "GitLab is ready."

# 通过 Personal Access Token 调用 API 创建 Runner token（GitLab 16.0+ 方式）
PAT=$(docker exec gitlab gitlab-rails runner \
  "u = User.find_by_username('root'); t = u.personal_access_tokens.create!(name: 'runner-setup-$(date +%s)', scopes: ['create_runner'], expires_at: 1.day.from_now); puts t.token" \
  2>/dev/null | tail -1)

RUNNER_TOKEN=$(curl -sf --request POST "${GITLAB_URL}/api/v4/user/runners" \
  --header "PRIVATE-TOKEN: ${PAT}" \
  --data "runner_type=instance_type" \
  --data "description=local-docker-runner" \
  --data "tag_list=local" \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['token'])")

docker exec gitlab-runner gitlab-runner register \
  --non-interactive \
  --url "${GITLAB_INTERNAL_URL}" \
  --token "${RUNNER_TOKEN}" \
  --executor docker \
  --docker-image alpine:latest \
  --description "local-docker-runner" \
  --docker-volumes /var/run/docker.sock:/var/run/docker.sock

echo "Runner registered successfully."
