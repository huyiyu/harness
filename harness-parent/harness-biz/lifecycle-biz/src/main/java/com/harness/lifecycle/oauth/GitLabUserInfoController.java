package com.harness.lifecycle.oauth;

import com.harness.auth.gitlab.GitLabApi;
import com.harness.auth.gitlab.dto.GitLabOAuthUserInfo;
import com.harness.auth.gitlab.dto.GitLabUserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class GitLabUserInfoController {

    private final GitLabApi gitLabApi;

    @GetMapping("/api/v4/user")
    public GitLabOAuthUserInfo userInfo(@AuthenticationPrincipal Jwt jwt) {
        String openid = jwt.getSubject();
        List<GitLabUserResponse> users = gitLabApi.findUsers("wechat_" + openid);
        if (users.isEmpty()) {
            throw new IllegalStateException("GitLab user not found for openid: " + openid);
        }
        GitLabUserResponse u = users.get(0);
        return new GitLabOAuthUserInfo(u.id(), u.username(), u.username(), u.email());
    }
}
