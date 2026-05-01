package com.harness.lifecycle.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.harness.auth.gitlab.GitLabApi;
import com.harness.lifecycle.auth.entity.WechatGitlabBinding;
import com.harness.lifecycle.auth.mapper.WechatGitlabBindingMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final GitLabApi gitLabApi;
    private final WechatGitlabBindingMapper bindingMapper;

    @Value("${gitlab.url}")
    private String gitlabUrl;

    public String login(String code) {
        String openid = code; // mock: code 即 openid

        long gitlabUserId = getOrCreateGitlabUser(openid);
        String token = createImpersonationToken(gitlabUserId);
        return gitlabUrl + "/?private_token=" + token;
    }

    private long getOrCreateGitlabUser(String openid) {
        WechatGitlabBinding binding = bindingMapper.selectOne(
            new LambdaQueryWrapper<WechatGitlabBinding>()
                .eq(WechatGitlabBinding::getOpenid, openid));

        if (binding != null) {
            return binding.getGitlabUserId();
        }

        String username = "wechat_" + openid;
        List<Map<String, Object>> existing = gitLabApi.findUsers(username);
        long userId = existing.isEmpty()
            ? createUser(openid, username)
            : ((Number) existing.get(0).get("id")).longValue();

        WechatGitlabBinding newBinding = new WechatGitlabBinding();
        newBinding.setOpenid(openid);
        newBinding.setGitlabUserId(userId);
        newBinding.setCreatedAt(LocalDateTime.now());
        bindingMapper.insert(newBinding);
        return userId;
    }

    private long createUser(String openid, String username) {
        String shortId = openid.length() > 8 ? openid.substring(0, 8) : openid;
        Map<String, Object> result = gitLabApi.createUser(Map.of(
            "username", username,
            "email", openid + "@wechat.mock",
            "name", "微信用户_" + shortId,
            "password", "Wechat@" + username,
            "skip_confirmation", true
        ));
        return ((Number) result.get("id")).longValue();
    }

    private String createImpersonationToken(long userId) {
        Map<String, Object> result = gitLabApi.createImpersonationToken(userId, Map.of(
            "name", "wechat-login",
            "scopes", List.of("api", "read_user")
        ));
        return (String) result.get("token");
    }
}
