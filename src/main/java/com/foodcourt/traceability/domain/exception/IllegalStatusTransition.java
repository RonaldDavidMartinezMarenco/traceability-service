package com.foodcourt.traceability.domain.exception;

/** The new status does not follow the stored history. Mapped to HTTP 409. */
public final class IllegalStatusTransition extends RuntimeException {

    public IllegalStatusTransition() {
        super("The status change is not allowed");
    }
}
