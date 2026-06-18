package com.taskmind.backend.relay;

import com.taskmind.backend.config.logging.RequestCorrelation;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(RelayClientProperties.class)
public class RelayClientConfig {
    @Bean
    public RestClient relayRestClient(
            RestClient.Builder builder, RelayClientProperties properties) {
        return builder.baseUrl(properties.baseUrl())
                .defaultHeader("X-TaskMind-Service-Token", properties.serviceToken())
                .requestInterceptor(
                        (request, body, execution) -> {
                            request.getHeaders()
                                    .set(
                                            RequestCorrelation.HEADER_NAME,
                                            RequestCorrelation.currentOrCreate());
                            return execution.execute(request, body);
                        })
                .build();
    }
}
