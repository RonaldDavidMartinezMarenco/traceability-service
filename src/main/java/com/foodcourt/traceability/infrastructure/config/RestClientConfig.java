package com.foodcourt.traceability.infrastructure.config;

import java.time.Duration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import com.foodcourt.traceability.infrastructure.output.client.BearerTokenRelay;

@Configuration
public class RestClientConfig {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(2);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(2);

    @Bean
    RestClient userServiceClient(ServiceClientsProperties clients, BearerTokenRelay bearerTokenRelay) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(CONNECT_TIMEOUT);
        factory.setReadTimeout(READ_TIMEOUT);
        return RestClient.builder()
                .baseUrl(clients.userServiceUrl())
                .requestFactory(factory)
                .requestInterceptor(bearerTokenRelay)
                .build();
    }
}
