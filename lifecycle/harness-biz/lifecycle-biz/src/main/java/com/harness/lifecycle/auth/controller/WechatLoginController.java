package com.harness.lifecycle.auth.controller;

import com.harness.auth.gitlab.GitLabApi;
import com.harness.auth.gitlab.dto.GitLabOAuthUserInfo;
import com.harness.auth.gitlab.dto.GitLabUserResponse;
import com.harness.lifecycle.auth.service.AuthService;
import com.harness.lifecycle.oauth.TokenRequest;
import com.harness.lifecycle.oauth.TokenStore;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class WechatLoginController {

    private final TokenStore tokenStore;
    private final AuthService authService;
    private final GitLabApi gitLabApi;

    @GetMapping("/mock/wechat/authorize")
    @ResponseBody
    public String mockAuthorize(@RequestParam("redirect_uri") String redirectUri,
                                @RequestParam(value = "state", defaultValue = "") String state) {
        return """
            <html><body style="font-family:sans-serif;text-align:center;padding:60px">
            <h2>微信授权登录（Mock）</h2>
            <form id="f" method="get" action="%s">
              <input type="hidden" name="state" value="%s"/>
              <input id="code" name="code" style="padding:8px;font-size:16px;width:240px"/><br/><br/>
              <button type="submit" style="padding:12px 32px;font-size:16px;cursor:pointer">授权登录</button>
            </form>
            <script>
              document.getElementById('code').value = 'mock_' + Math.random().toString(36).slice(2,10);
            </script>
            </body></html>
            """.formatted(redirectUri, state);
    }

    @GetMapping("/oauth/authorize")
    public void oauthAuthorize(@RequestParam String redirect_uri,
                               @RequestParam(required = false) String state,
                               HttpServletResponse response) throws IOException {
        String wechatState = tokenStore.savePending(redirect_uri, state);
        response.sendRedirect("/mock/wechat/authorize?redirect_uri=http://lifecycle.harnesss.com/oauth/wechat/callback&state=" + wechatState);
    }

    @GetMapping("/oauth/wechat/callback")
    public void oauthWechatCallback(@RequestParam String code,
                                    @RequestParam String state,
                                    HttpServletResponse response) throws IOException {
        String[] parts = tokenStore.removePending(state);
        if (parts == null) {
            response.sendError(400, "invalid state");
            return;
        }
        String authCode = tokenStore.saveCode(code);
        String location = parts[0] + "?code=" + authCode + (!parts[1].isEmpty() ? "&state=" + parts[1] : "");
        response.sendRedirect(location);
    }

    @PostMapping(value = "/oauth/token", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    @ResponseBody
    public Map<String, Object> oauthToken(@ModelAttribute TokenRequest req) {
        String openid = tokenStore.removeCode(req.code());
        if (openid == null) {
            throw new IllegalArgumentException("invalid code");
        }
        return Map.of("access_token", tokenStore.saveToken(openid), "token_type", "bearer");
    }

    @GetMapping("/api/v4/user")
    @ResponseBody
    public GitLabOAuthUserInfo userInfo(@RequestHeader("Authorization") String authorization) {
        String token = authorization.replaceFirst("(?i)bearer ", "");
        String openid = tokenStore.getOpenid(token);
        if (openid == null) {
            throw new IllegalArgumentException("invalid token");
        }
        authService.getOrCreateGitlabUser(openid);
        List<GitLabUserResponse> users = gitLabApi.findUsers("wechat_" + openid);
        GitLabUserResponse u = users.get(0);
        return new GitLabOAuthUserInfo(u.id(), u.username(), u.username(), u.email());
    }
}
