package com.foodcourt.traceability.domain.exception;

/** This status is already stored for the order with different identity fields. Mapped to HTTP 409. */
public final class TraceConflict extends RuntimeException {

    public TraceConflict() {
        super("The order already has a different record for this status");
    }
}
