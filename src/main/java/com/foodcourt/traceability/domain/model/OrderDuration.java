package com.foodcourt.traceability.domain.model;

import java.time.Instant;

/**
 * One finished order on the owner's page.
 * {@code slowerThanAverage} uses the restaurant average of every finished order, not the page.
 */
public record OrderDuration(
        long orderId,
        long employeeId,
        Instant startedAt,
        Instant finishedAt,
        long durationSeconds,
        boolean slowerThanAverage) {
}
