package com.harness.lifecycle.auth.controller;

import com.harness.lifecycle.auth.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerTest {

    private MockMvc mockMvc;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = Mockito.mock(AuthService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new AuthController(authService)).build();
    }

    @Test
    void login_redirectsToMockWechat() throws Exception {
        mockMvc.perform(get("/auth/wechat/login"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(
                        "/mock/wechat/authorize?redirect_uri=http://localhost:8081/auth/wechat/callback&state=login"));
    }

    @Test
    void callback_redirectsToGitlab() throws Exception {
        when(authService.login("test_code")).thenReturn("http://gitlab:8080/?private_token=abc");

        mockMvc.perform(get("/auth/wechat/callback").param("code", "test_code"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://gitlab:8080/?private_token=abc"));
    }
}
