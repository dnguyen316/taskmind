package com.taskmind.backend.relay;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(RelayClientProperties.class)
public class RelayClientConfig {
    @Bean
    RestClient relayRestClient(RestClient.Builder builder, RelayClientProperties properties) {
        return builder.baseUrl(properties.baseUrl())
                .defaultHeader("X-TaskMind-Service-Token", properties.serviceToken())
                .build();
    }
}
