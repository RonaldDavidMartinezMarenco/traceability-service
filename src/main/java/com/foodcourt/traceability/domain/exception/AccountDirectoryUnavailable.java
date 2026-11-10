package com.foodcourt.traceability.domain.exception;

/** user-service did not answer the account lookup. Mapped to HTTP 503. */
public final class AccountDirectoryUnavailable extends RuntimeException {

    public AccountDirectoryUnavailable() {
        super("The user service is unavailable");
    }
}
