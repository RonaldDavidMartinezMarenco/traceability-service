package com.foodcourt.traceability.infrastructure.input.rest.dto;

import java.util.List;

/** Status history of one order, oldest first. */
public record OrderTraceResponse(
        long orderId,
        long customerId,
        long restaurantId,
        List<TraceEntryResponse> changes) {
}
