package com.foodcourt.traceability.domain.model;

import com.foodcourt.traceability.domain.exception.InvalidTraceRequest;

/** Order states. The stored name uses these capitals. Anything else is rejected. */
public enum OrderStatus {
    PENDING,
    IN_PREPARATION,
    READY,
    DELIVERED,
    CANCELLED;

    /** A missing status is rejected. The text is not trimmed. */
    public static OrderStatus parseExact(String raw) {
        if (raw == null) {
            throw new InvalidTraceRequest("The status is required");
        }
        return known(raw);
    }

    /** A missing previous status is the opening of an order. Any other text must be a known state. */
    public static OrderStatus parseOptional(String raw) {
        if (raw == null) {
            return null;
        }
        return known(raw);
    }

    private static OrderStatus known(String raw) {
        try {
            return OrderStatus.valueOf(raw);
        } catch (IllegalArgumentException failure) {
            throw new InvalidTraceRequest("The status is unknown");
        }
    }
}
