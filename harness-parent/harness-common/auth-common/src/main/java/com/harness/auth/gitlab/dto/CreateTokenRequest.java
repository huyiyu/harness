package com.harness.auth.gitlab.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record CreateTokenRequest(
        String name,
        List<String> scopes,
        @JsonProperty("expires_at") String expiresAt) {}
