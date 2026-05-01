CREATE TABLE IF NOT EXISTS wechat_gitlab_binding (
  openid         VARCHAR(64)  NOT NULL PRIMARY KEY,
  gitlab_user_id BIGINT       NOT NULL,
  created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
);
