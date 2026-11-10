package com.foodcourt.traceability.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "foodcourt.clients")
public record ServiceClientsProperties(String userServiceUrl) {
}
