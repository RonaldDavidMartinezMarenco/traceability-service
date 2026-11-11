package com.foodcourt.traceability.domain.model;

import com.foodcourt.traceability.domain.exception.InvalidPageRequest;

/**
 * Zero-based page request. {@code size} is at most {@link #MAX_SIZE}, and
 * {@code page * size} must fit in an {@code int}: Spring Data rejects a larger offset.
 */
public record PageQuery(int page, int size) {

    public static final int MAX_SIZE = 50;

    public PageQuery {
        if (page < 0 || size < 1 || size > MAX_SIZE || overflows(page, size)) {
            throw new InvalidPageRequest();
        }
    }

    private static boolean overflows(int page, int size) {
        return (long) page * size > Integer.MAX_VALUE;
    }
}
