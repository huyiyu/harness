package com.harness.lifecycle.auth.controller;

import com.harness.lifecycle.auth.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@Controller
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @GetMapping("/wechat/login")
    public void login(HttpServletResponse response) throws IOException {
        String callbackUri = "http://localhost:8081/auth/wechat/callback";
        response.sendRedirect("/mock/wechat/authorize?redirect_uri=" + callbackUri + "&state=login");
    }

    @GetMapping("/wechat/callback")
    public void callback(@RequestParam("code") String code,
                         HttpServletResponse response) throws IOException {
        String redirectUrl = authService.login(code);
        response.sendRedirect(redirectUrl);
    }
}
