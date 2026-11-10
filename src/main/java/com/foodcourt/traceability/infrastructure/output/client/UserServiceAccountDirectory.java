package com.foodcourt.traceability.infrastructure.output.client;

import java.io.IOException;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.foodcourt.traceability.domain.exception.AccessDenied;
import com.foodcourt.traceability.domain.exception.AccountDirectoryUnavailable;
import com.foodcourt.traceability.domain.model.Account;
import com.foodcourt.traceability.domain.model.Role;
import com.foodcourt.traceability.domain.spi.AccountDirectory;

/**
 * Reads {@code GET /users/{id}} from user-service.
 * HTTP 404 is an empty account. HTTP 401 and 403 reject the caller. Any other failure is unavailable.
 */
@Service
public final class UserServiceAccountDirectory implements AccountDirectory {

    private final RestClient userService;

    public UserServiceAccountDirectory(@Qualifier("userServiceClient") RestClient userService) {
        this.userService = userService;
    }

    @Override
    public Optional<Account> findById(long id) {
        try {
            return userService.get().uri("/users/{id}", id).exchange(this::read);
        } catch (RestClientException failure) {
            throw new AccountDirectoryUnavailable();
        }
    }

    private Optional<Account> read(
            HttpRequest request,
            RestClient.RequestHeadersSpec.ConvertibleClientHttpResponse response) throws IOException {
        HttpStatusCode status = response.getStatusCode();
        if (isNotFound(status)) {
            return Optional.empty();
        }
        if (isRejected(status)) {
            throw new AccessDenied();
        }
        if (!status.is2xxSuccessful()) {
            throw new AccountDirectoryUnavailable();
        }
        return Optional.of(account(response.bodyTo(AccountPayload.class)));
    }

    private static boolean isNotFound(HttpStatusCode status) {
        return status.value() == 404;
    }

    private static boolean isRejected(HttpStatusCode status) {
        return status.value() == 401 || status.value() == 403;
    }

    private static Account account(AccountPayload body) {
        if (body == null || body.id() == null || body.role() == null) {
            throw new AccountDirectoryUnavailable();
        }
        return new Account(body.id(), body.role(), body.restaurantId());
    }

    /** Body of {@code GET /users/{id}}. The domain does not depend on this type. */
    record AccountPayload(Long id, Role role, Long restaurantId) {
    }
}
