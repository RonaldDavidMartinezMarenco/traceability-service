package com.foodcourt.traceability.infrastructure.output.client;

import java.io.IOException;

import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

/** Copies the caller's bearer token onto the outbound request. */
@Component
public final class BearerTokenRelay implements ClientHttpRequestInterceptor {

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
            throws IOException {
        copyBearer(request, SecurityContextHolder.getContext().getAuthentication());
        return execution.execute(request, body);
    }

    private static void copyBearer(HttpRequest request, Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            request.getHeaders().setBearerAuth(jwt.getTokenValue());
        }
    }
}
