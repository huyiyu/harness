package com.harness.auth.gitlab;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

import java.util.List;
import java.util.Map;

@HttpExchange("/api/v4")
public interface GitLabApi {

    @GetExchange("/users")
    List<Map<String, Object>> findUsers(@RequestParam("username") String username);

    @PostExchange("/users")
    Map<String, Object> createUser(@RequestBody Map<String, Object> body);

    @PostExchange("/users/{id}/impersonation_tokens")
    Map<String, Object> createImpersonationToken(@PathVariable("id") long userId,
                                                  @RequestBody Map<String, Object> body);
}
