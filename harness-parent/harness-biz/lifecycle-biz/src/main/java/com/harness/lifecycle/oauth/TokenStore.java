package com.harness.lifecycle.oauth;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class TokenStore {

    private final Map<String, String> pending = new ConcurrentHashMap<>();
    private final Map<String, String> codes = new ConcurrentHashMap<>();
    private final Map<String, String> tokens = new ConcurrentHashMap<>();

    public String savePending(String redirectUri, String state) {
        String key = UUID.randomUUID().toString();
        pending.put(key, redirectUri + "|" + (state != null ? state : ""));
        return key;
    }

    public String[] removePending(String key) {
        String stored = pending.remove(key);
        return stored != null ? stored.split("\\|", 2) : null;
    }

    public String saveCode(String openid) {
        String code = UUID.randomUUID().toString();
        codes.put(code, openid);
        return code;
    }

    public String removeCode(String code) {
        return codes.remove(code);
    }

    public String saveToken(String openid) {
        String token = UUID.randomUUID().toString();
        tokens.put(token, openid);
        return token;
    }

    public String getOpenid(String token) {
        return tokens.get(token);
    }
}
