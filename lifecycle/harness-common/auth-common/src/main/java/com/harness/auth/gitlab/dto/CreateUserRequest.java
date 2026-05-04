package com.harness.auth.gitlab.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

@Builder
public record CreateUserRequest(
    String username,
    String email,
    String name,
    String password,
    @JsonProperty("skip_confirmation") boolean skipConfirmation
) { }
