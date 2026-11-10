package com.foodcourt.traceability.domain.exception;

/** The request has no usable access token. Mapped to HTTP 401. */
public final class AuthenticationRequired extends RuntimeException {

    public AuthenticationRequired() {
        super("Authentication is required");
    }
}
