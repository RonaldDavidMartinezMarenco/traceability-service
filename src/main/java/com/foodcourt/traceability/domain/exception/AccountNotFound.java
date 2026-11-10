package com.foodcourt.traceability.domain.exception;

/** user-service has no account for the caller. Mapped to HTTP 404. */
public final class AccountNotFound extends RuntimeException {

    public AccountNotFound() {
        super("No account exists for this id");
    }
}
