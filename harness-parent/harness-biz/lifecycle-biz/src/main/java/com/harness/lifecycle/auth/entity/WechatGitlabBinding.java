package com.harness.lifecycle.auth.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("wechat_gitlab_binding")
public class WechatGitlabBinding {

    @TableId
    private String openid;
    private Long gitlabUserId;
    private String initialPassword;
    private LocalDateTime createdAt;
}
