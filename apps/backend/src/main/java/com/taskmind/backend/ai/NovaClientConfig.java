package com.taskmind.backend.ai;

import com.taskmind.backend.config.logging.RequestCorrelation;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(NovaClientProperties.class)
public class NovaClientConfig {
    @Bean
    public RestClient novaRestClient(RestClient.Builder builder, NovaClientProperties properties) {
        return builder.baseUrl(properties.baseUrl())
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
