package com.foodcourt.traceability.infrastructure.config;

import java.nio.charset.StandardCharsets;
import java.util.List;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

/**
 * Validates the HS256 tokens issued by user-service. This service does not sign tokens.
 * A secret shorter than 32 bytes fails startup. This package is excluded from coverage.
 */
@Configuration
public class JwtConfig {

    public static final String ISSUER = "user-service";
    public static final String AUDIENCE = "foodcourt";
    public static final String ROLE_CLAIM = "role";
    private static final int SECRET_MIN_BYTES = 32;

    @Bean
    SecretKey jwtSecret(@Value("${foodcourt.security.jwt-secret}") String secret) {
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < SECRET_MIN_BYTES) {
            throw new IllegalStateException("JWT_SECRET must be at least 32 bytes");
        }
        return new SecretKeySpec(bytes, "HmacSHA256");
    }

    @Bean
    JwtDecoder jwtDecoder(SecretKey jwtSecret) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(jwtSecret)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        decoder.setJwtValidator(validator());
        return decoder;
    }

    private static OAuth2TokenValidator<Jwt> validator() {
        return new DelegatingOAuth2TokenValidator<>(issuer(), audience());
    }

    private static OAuth2TokenValidator<Jwt> issuer() {
        return JwtValidators.createDefaultWithIssuer(ISSUER);
    }

    private static OAuth2TokenValidator<Jwt> audience() {
        return new JwtClaimValidator<>(JwtClaimNames.AUD, JwtConfig::audienceMatches);
    }

    private static boolean audienceMatches(List<String> audience) {
        return audience != null && audience.contains(AUDIENCE);
    }
}
