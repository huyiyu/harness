package com.harness.auth.gitlab.dto;

import java.util.List;

public record CreateTokenRequest(String name, List<String> scopes) {}
