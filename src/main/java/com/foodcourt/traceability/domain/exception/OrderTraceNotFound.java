package com.foodcourt.traceability.domain.exception;

/** This customer has no status history for the order. Mapped to HTTP 404. */
public final class OrderTraceNotFound extends RuntimeException {

    public OrderTraceNotFound() {
        super("No trace exists for this order");
    }
}
