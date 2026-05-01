package com.harness.lifecycle.auth.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class MockWechatController {

    @GetMapping("/mock/wechat/authorize")
    @ResponseBody
    public String authorize(@RequestParam("redirect_uri") String redirectUri,
                            @RequestParam(value = "state", defaultValue = "") String state) {
        String callbackUrl = redirectUri + "?code=mock_openid_001&state=" + state;
        return """
            <html><body style="font-family:sans-serif;text-align:center;padding:60px">
            <h2>微信授权登录（Mock）</h2>
            <p>当前用户：mock_openid_001</p>
            <a href="%s"><button style="padding:12px 32px;font-size:16px;cursor:pointer">授权登录</button></a>
            </body></html>
            """.formatted(callbackUrl);
    }
}
