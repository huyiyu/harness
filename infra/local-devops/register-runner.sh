#!/bin/bash
set -e

source "$(dirname "$0")/.env"

GITLAB_URL="http://localhost:${GITLAB_PORT}"

echo "Waiting for GitLab to be ready..."
until curl -sf "${GITLAB_URL}/-/health" > /dev/null; do
  sleep 5
done
echo "GitLab is ready."

TOKEN=$(docker exec gitlab gitlab-rails runner \
  "puts Gitlab::CurrentSettings.current_application_settings.runners_registration_token" 2>/dev/null)

docker exec gitlab-runner gitlab-runner register \
  --non-interactive \
  --url "${GITLAB_URL}" \
  --registration-token "${TOKEN}" \
  --executor docker \
  --docker-image alpine:latest \
  --description "local-docker-runner" \
  --tag-list "local" \
  --docker-volumes /var/run/docker.sock:/var/run/docker.sock

echo "Runner registered successfully."
