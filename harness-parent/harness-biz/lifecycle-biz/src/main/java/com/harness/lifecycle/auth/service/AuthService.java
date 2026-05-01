package com.harness.lifecycle.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.harness.auth.gitlab.GitLabApi;
import com.harness.auth.gitlab.GitLabProperties;
import com.harness.auth.gitlab.PasswordGenerator;
import com.harness.auth.gitlab.dto.CreateTokenRequest;
import com.harness.auth.gitlab.dto.CreateUserRequest;
import com.harness.lifecycle.auth.entity.WechatGitlabBinding;
import com.harness.lifecycle.auth.mapper.WechatGitlabBindingMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final GitLabApi gitLabApi;
    private final WechatGitlabBindingMapper bindingMapper;
    private final GitLabProperties gitLabProperties;

    public String login(String code) {
        String openid = code;
        long gitlabUserId = getOrCreateGitlabUser(openid);
        String expiresAt = LocalDate.now().plusYears(1).toString();
        String token = gitLabApi.createImpersonationToken(gitlabUserId,
            new CreateTokenRequest("wechat-login", List.of("api", "read_user"), expiresAt)).token();
        return gitLabProperties.getUrl() + "/?private_token=" + token;
    }

    public long getOrCreateGitlabUser(String openid) {
        WechatGitlabBinding binding = bindingMapper.selectOne(
            new LambdaQueryWrapper<WechatGitlabBinding>()
                .eq(WechatGitlabBinding::getOpenid, openid));
        if (binding != null) {
            return binding.getGitlabUserId();
        }

        String username = "wechat_" + openid;
        var existing = gitLabApi.findUsers(username);
        String password = PasswordGenerator.generate();
        long userId = existing.isEmpty()
            ? gitLabApi.createUser(buildCreateUserRequest(openid, username, password)).id()
            : existing.get(0).id();

        WechatGitlabBinding newBinding = new WechatGitlabBinding();
        newBinding.setOpenid(openid);
        newBinding.setGitlabUserId(userId);
        newBinding.setInitialPassword(password);
        newBinding.setCreatedAt(LocalDateTime.now());
        bindingMapper.insert(newBinding);
        return userId;
    }

    private CreateUserRequest buildCreateUserRequest(String openid, String username, String password) {
        String shortId = openid.length() > 8 ? openid.substring(0, 8) : openid;
        return CreateUserRequest.builder()
            .username(username)
            .email(openid + "@wechat.mock")
            .name("微信用户_" + shortId)
            .password(password)
            .skipConfirmation(true)
            .build();
    }
}
