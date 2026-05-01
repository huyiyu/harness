package com.harness.auth.gitlab;

import com.harness.auth.gitlab.dto.CreateTokenRequest;
import com.harness.auth.gitlab.dto.CreateUserRequest;
import com.harness.auth.gitlab.dto.GitLabUserResponse;
import com.harness.auth.gitlab.dto.ImpersonationTokenResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

import java.util.List;

@HttpExchange("/api/v4")
public interface GitLabApi {

    @GetExchange("/users")
    List<GitLabUserResponse> findUsers(@RequestParam("username") String username);

    @PostExchange("/users")
    GitLabUserResponse createUser(@RequestBody CreateUserRequest body);

    @PostExchange("/users/{id}/impersonation_tokens")
    ImpersonationTokenResponse createImpersonationToken(@PathVariable("id") long userId,
                                                         @RequestBody CreateTokenRequest body);
}
