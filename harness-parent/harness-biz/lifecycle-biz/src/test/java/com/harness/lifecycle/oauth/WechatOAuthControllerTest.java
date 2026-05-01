package com.harness.lifecycle.oauth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class WechatOAuthControllerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        WechatUserDetailsService userDetailsService = Mockito.mock(WechatUserDetailsService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(
                new WechatOAuthController(userDetailsService)).build();
    }

    @Test
    void authorize_redirectsToMockWechat() throws Exception {
        mockMvc.perform(get("/oauth/authorize?client_id=gitlab-client&response_type=code&redirect_uri=http://localhost:8080/users/auth/oauth2_generic/callback"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", startsWith("/mock/wechat/authorize")));
    }

    @Test
    void wechatCallback_invalidState_returns400() throws Exception {
        mockMvc.perform(get("/oauth/wechat/callback")
                        .param("code", "mock_openid_001")
                        .param("state", "nonexistent-state"))
                .andExpect(status().isBadRequest());
    }
}
