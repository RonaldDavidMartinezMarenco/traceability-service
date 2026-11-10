package com.foodcourt.traceability.infrastructure.input.rest.dto;

import java.time.Instant;

import com.foodcourt.traceability.domain.model.OrderStatus;

/**
 * One step of a customer's trace.
 * {@code secondsInStatus} is null while this is the current status.
 */
public record TraceEntryResponse(
        OrderStatus previousStatus,
        OrderStatus newStatus,
        Instant occurredAt,
        Long secondsInStatus) {
}
