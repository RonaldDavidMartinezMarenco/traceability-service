package com.foodcourt.traceability.domain.exception;

/** The status text or the employee does not fit the requested change. Mapped to HTTP 400. */
public final class InvalidTraceRequest extends RuntimeException {

    public InvalidTraceRequest(String message) {
        super(message);
    }
}
