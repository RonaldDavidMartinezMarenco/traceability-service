package com.foodcourt.traceability.domain.exception;

/** Page index, page size, or their product is outside the supported range. Mapped to HTTP 400. */
public final class InvalidPageRequest extends RuntimeException {

    public InvalidPageRequest() {
        super("The page must be zero or more, the size between 1 and 50, and page times size must not exceed 2147483647");
    }
}
