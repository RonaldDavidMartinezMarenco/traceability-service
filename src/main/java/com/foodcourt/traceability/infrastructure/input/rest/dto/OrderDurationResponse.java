package com.foodcourt.traceability.infrastructure.input.rest.dto;

import java.time.Instant;

/** One finished order on the efficiency page. */
public record OrderDurationResponse(
        long orderId,
        long employeeId,
        Instant startedAt,
        Instant finishedAt,
        long durationSeconds,
        boolean slowerThanAverage) {
}
