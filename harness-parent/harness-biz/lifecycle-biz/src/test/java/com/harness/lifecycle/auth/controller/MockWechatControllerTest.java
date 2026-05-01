package com.harness.lifecycle.auth.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MockWechatControllerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new MockWechatController()).build();
    }

    @Test
    void authorize_returnsHtmlWithCallbackLink() throws Exception {
        mockMvc.perform(get("/mock/wechat/authorize")
                        .param("redirect_uri", "http://localhost:8081/auth/wechat/callback")
                        .param("state", "login"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "http://localhost:8081/auth/wechat/callback?code=mock_openid_001&state=login")));
    }
}
