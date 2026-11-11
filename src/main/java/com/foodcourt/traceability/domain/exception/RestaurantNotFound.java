package com.foodcourt.traceability.domain.exception;

/** plaza-service has no restaurant for this id. Mapped to HTTP 404. */
public final class RestaurantNotFound extends RuntimeException {

    public RestaurantNotFound() {
        super("No restaurant exists for this id");
    }
}
