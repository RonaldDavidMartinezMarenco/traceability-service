package com.foodcourt.traceability.infrastructure.input.rest.dto;

import java.time.Instant;

import com.foodcourt.traceability.domain.model.OrderStatus;

/** One stored status change. The employee id stays out of the response. */
public record StatusChangeResponse(
        long orderId,
        long customerId,
        long restaurantId,
        OrderStatus previousStatus,
        OrderStatus newStatus,
        Instant occurredAt) {
}
