package com.harness.lifecycle.oauth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Controller
@RequiredArgsConstructor
public class WechatOAuthController {

    private final WechatUserDetailsService userDetailsService;

    private final Map<String, String> pendingAuthorizations = new ConcurrentHashMap<>();

    @GetMapping("/oauth/authorize")
    public void authorize(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String originalQuery = request.getQueryString();
        String wechatState = UUID.randomUUID().toString();
        pendingAuthorizations.put(wechatState, originalQuery);

        String callbackUri = "http://localhost:8081/oauth/wechat/callback";
        response.sendRedirect("/mock/wechat/authorize?redirect_uri=" + callbackUri + "&state=" + wechatState);
    }

    @GetMapping("/oauth/wechat/callback")
    public void wechatCallback(@RequestParam("code") String openid,
                               @RequestParam("state") String wechatState,
                               HttpServletRequest request,
                               HttpServletResponse response) throws IOException {
        String originalQuery = pendingAuthorizations.remove(wechatState);
        if (originalQuery == null) {
            response.sendError(400, "Invalid state");
            return;
        }

        UserDetails user = userDetailsService.loadUserByUsername(openid);
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        response.sendRedirect("/oauth/authorize?" + originalQuery);
    }
}
