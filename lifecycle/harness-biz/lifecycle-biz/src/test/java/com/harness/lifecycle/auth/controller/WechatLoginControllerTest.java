package com.harness.lifecycle.auth.controller;

import com.harness.auth.gitlab.GitLabApi;
import com.harness.auth.gitlab.dto.GitLabUserResponse;
import com.harness.lifecycle.auth.service.AuthService;
import com.harness.lifecycle.oauth.TokenStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class WechatLoginControllerTest {

    private MockMvc mockMvc;
    private TokenStore tokenStore;
    private AuthService authService;
    private GitLabApi gitLabApi;

    @BeforeEach
    void setUp() {
        tokenStore = new TokenStore();
        authService = Mockito.mock(AuthService.class);
        gitLabApi = Mockito.mock(GitLabApi.class);
        WechatLoginController controller = new WechatLoginController(tokenStore, authService, gitLabApi);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void mockAuthorize_returnsHtmlWithCallbackLink() throws Exception {
        mockMvc.perform(get("/mock/wechat/authorize")
                        .param("redirect_uri", "http://localhost:8081/oauth/wechat/callback")
                        .param("state", "login"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("action=\"http://localhost:8081/oauth/wechat/callback\"")))
                .andExpect(content().string(containsString("name=\"code\"")));
    }

    @Test
    void oauthAuthorize_redirectsToMockWechat() throws Exception {
        mockMvc.perform(get("/oauth/authorize")
                        .param("redirect_uri", "http://localhost:8080/users/auth/oauth2_generic/callback")
                        .param("state", "abc"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", startsWith("/mock/wechat/authorize")));
    }

    @Test
    void oauthWechatCallback_validState_redirectsWithAuthCode() throws Exception {
        MvcResult result = mockMvc.perform(get("/oauth/authorize")
                        .param("redirect_uri", "http://localhost:8080/users/auth/oauth2_generic/callback")
                        .param("state", "abc"))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        String location = result.getResponse().getHeader("Location");
        String wechatState = UriComponentsBuilder.fromUriString(location).build().getQueryParams().getFirst("state");

        mockMvc.perform(get("/oauth/wechat/callback")
                        .param("code", "mock_openid_001")
                        .param("state", wechatState))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", startsWith("http://localhost:8080/users/auth/oauth2_generic/callback?code=")));
    }

    @Test
    void oauthToken_validCode_returnsAccessToken() throws Exception {
        String code = tokenStore.saveCode("mock_openid_001");

        mockMvc.perform(post("/oauth/token")
                        .contentType(org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED)
                        .content("grant_type=authorization_code&code=" + code + "&client_id=test-client&client_secret=test-secret"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token_type").value("bearer"))
                .andExpect(jsonPath("$.access_token").exists());
    }

    @Test
    void userInfo_validToken_returnsGitLabUserInfo() throws Exception {
        String token = tokenStore.saveToken("mock_openid_001");

        when(authService.getOrCreateGitlabUser("mock_openid_001")).thenReturn(3L);
        when(gitLabApi.findUsers("wechat_mock_openid_001"))
                .thenReturn(List.of(new GitLabUserResponse(3L, "wechat_mock_openid_001", "mock_openid_001@wechat.mock")));

        mockMvc.perform(get("/api/v4/user")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(3))
                .andExpect(jsonPath("$.username").value("wechat_mock_openid_001"));
    }
}
