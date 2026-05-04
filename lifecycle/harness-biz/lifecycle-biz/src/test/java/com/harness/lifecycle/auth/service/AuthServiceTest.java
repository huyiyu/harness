package com.harness.lifecycle.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.harness.auth.gitlab.GitLabApi;
import com.harness.auth.gitlab.GitLabProperties;
import com.harness.auth.gitlab.dto.CreateTokenRequest;
import com.harness.auth.gitlab.dto.CreateUserRequest;
import com.harness.auth.gitlab.dto.GitLabUserResponse;
import com.harness.auth.gitlab.dto.ImpersonationTokenResponse;
import com.harness.lifecycle.auth.entity.WechatGitlabBinding;
import com.harness.lifecycle.auth.mapper.WechatGitlabBindingMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private GitLabApi gitLabApi;
    @Mock
    private WechatGitlabBindingMapper bindingMapper;
    @Mock
    private GitLabProperties gitLabProperties;

    @InjectMocks
    private AuthService authService;

    @BeforeEach
    void setUp() {
        when(gitLabProperties.getUrl()).thenReturn("http://localhost:8080");
        when(gitLabApi.createImpersonationToken(anyLong(), any(CreateTokenRequest.class)))
            .thenReturn(new ImpersonationTokenResponse("test-token"));
    }

    @Test
    void login_existingBinding_reusesGitlabUser() {
        WechatGitlabBinding binding = new WechatGitlabBinding();
        binding.setGitlabUserId(42L);
        when(bindingMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(binding);

        String result = authService.login("existing_openid");

        assertThat(result).isEqualTo("http://localhost:8080/?private_token=test-token");
        verify(gitLabApi, never()).createUser(any());
    }

    @Test
    void login_noBinding_createsGitlabUserAndBinding() {
        List<WechatGitlabBinding> inserted = new ArrayList<>();
        when(bindingMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(gitLabApi.findUsers("wechat_new_openid")).thenReturn(List.of());
        when(gitLabApi.createUser(any(CreateUserRequest.class)))
            .thenReturn(new GitLabUserResponse(99L, "wechat_new_openid", "new_openid@wechat.mock"));
        doAnswer(inv -> {
            inserted.add(inv.getArgument(0));
            return 1;
        }).when(bindingMapper).insert(any(WechatGitlabBinding.class));

        String result = authService.login("new_openid");

        assertThat(result).startsWith("http://localhost:8080/?private_token=");
        assertThat(inserted).hasSize(1);
        assertThat(inserted.get(0).getOpenid()).isEqualTo("new_openid");
        assertThat(inserted.get(0).getGitlabUserId()).isEqualTo(99L);
    }

    @Test
    void login_noBinding_gitlabUserAlreadyExists_skipsCreation() {
        List<WechatGitlabBinding> inserted = new ArrayList<>();
        when(bindingMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(gitLabApi.findUsers("wechat_dup_openid"))
            .thenReturn(List.of(new GitLabUserResponse(77L, "wechat_dup_openid", "dup@wechat.mock")));
        doAnswer(inv -> {
            inserted.add(inv.getArgument(0));
            return 1;
        }).when(bindingMapper).insert(any(WechatGitlabBinding.class));

        authService.login("dup_openid");

        verify(gitLabApi, never()).createUser(any());
        assertThat(inserted).hasSize(1);
        assertThat(inserted.get(0).getGitlabUserId()).isEqualTo(77L);
    }
}
