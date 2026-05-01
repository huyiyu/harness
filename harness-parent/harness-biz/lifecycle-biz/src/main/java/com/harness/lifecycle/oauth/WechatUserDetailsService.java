package com.harness.lifecycle.oauth;

import com.harness.auth.gitlab.GitLabApi;
import com.harness.lifecycle.auth.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WechatUserDetailsService implements UserDetailsService {

    private final AuthService authService;
    private final GitLabApi gitLabApi;

    @Override
    public UserDetails loadUserByUsername(String openid) throws UsernameNotFoundException {
        authService.getOrCreateGitlabUser(openid);
        var gitlabUser = gitLabApi.findUsers("wechat_" + openid);
        String username = gitlabUser.isEmpty() ? "wechat_" + openid : gitlabUser.get(0).username();
        return User.withUsername(openid)
                .password("{noop}" + username)
                .roles("USER")
                .build();
    }
}
