package com.foodcourt.traceability.domain.exception;

/** plaza-service did not answer the restaurant lookup. Mapped to HTTP 503. */
public final class RestaurantDirectoryUnavailable extends RuntimeException {

    public RestaurantDirectoryUnavailable() {
        super("The plaza service is unavailable");
    }
}
