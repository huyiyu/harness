package com.harness.auth.gitlab;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@AutoConfiguration
@EnableConfigurationProperties(GitLabProperties.class)
public class GitLabAutoConfiguration {

    @Bean
    public GitLabApi gitLabApi(GitLabProperties props) {
        RestClient restClient = RestClient.builder()
            .baseUrl(props.getUrl())
            .defaultHeader("PRIVATE-TOKEN", props.getAdminToken())
            .build();
        return HttpServiceProxyFactory
            .builderFor(RestClientAdapter.create(restClient))
            .build()
            .createClient(GitLabApi.class);
    }
}
