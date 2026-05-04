package com.harness.auth.gitlab;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "gitlab")
public class GitLabProperties {
    private String url;
    private String adminToken;
}
