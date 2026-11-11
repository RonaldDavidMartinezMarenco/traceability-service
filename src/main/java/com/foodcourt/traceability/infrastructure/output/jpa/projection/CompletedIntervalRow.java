package com.foodcourt.traceability.infrastructure.output.jpa.projection;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Optional;

import com.foodcourt.traceability.domain.model.CompletedInterval;
import com.foodcourt.traceability.domain.model.OrderStatus;
import com.foodcourt.traceability.domain.model.StatusChange;

/**
 * One row of the finished-order query, before {@link CompletedInterval} accepts it.
 * Columns are order id, restaurant id, employee id, preparation time, and delivery time.
 */
public record CompletedIntervalRow(
        long orderId,
        long restaurantId,
        long employeeId,
        Instant startedAt,
        Instant finishedAt) {

    public static CompletedIntervalRow from(Object[] row) {
        return new CompletedIntervalRow(
                number(row[0]),
                number(row[1]),
                number(row[2]),
                instant(row[3]),
                instant(row[4]));
    }

    /** Preparation and delivery of this row. Empty when the pair is not a finished order. */
    public Optional<CompletedInterval> accept() {
        return CompletedInterval.between(
                change(OrderStatus.IN_PREPARATION, startedAt),
                change(OrderStatus.DELIVERED, finishedAt));
    }

    private StatusChange change(OrderStatus status, Instant occurredAt) {
        return new StatusChange(null, orderId, 0L, restaurantId, employeeId, null, status, occurredAt, null);
    }

    private static long number(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        throw new IllegalStateException("The efficiency query returned an unexpected number");
    }

    private static Instant instant(Object value) {
        if (value instanceof Instant direct) {
            return direct;
        }
        if (value instanceof OffsetDateTime offset) {
            return offset.toInstant();
        }
        if (value instanceof Timestamp timestamp) {
            return timestamp.toInstant();
        }
        throw new IllegalStateException("The efficiency query returned an unexpected timestamp");
    }
}
