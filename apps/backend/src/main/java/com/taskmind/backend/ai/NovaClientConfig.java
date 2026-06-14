package com.taskmind.backend.ai;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(NovaClientProperties.class)
public class NovaClientConfig {
    @Bean
    RestClient novaRestClient(RestClient.Builder builder, NovaClientProperties properties) {
        return builder.baseUrl(properties.baseUrl()).build();
    }
}
