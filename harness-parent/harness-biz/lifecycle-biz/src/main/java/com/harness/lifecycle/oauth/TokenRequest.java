package com.harness.lifecycle.oauth;

public record TokenRequest(String grant_type, String code, String client_id, String client_secret, String redirect_uri) {}
