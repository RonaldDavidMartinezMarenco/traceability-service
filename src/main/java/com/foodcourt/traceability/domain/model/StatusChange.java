package com.foodcourt.traceability.domain.model;

import java.time.Instant;
import java.util.Objects;

/**
 * One stored change of an order. {@code secondsInStatus} is filled only when a trace is read.
 * It is the gap until the next change, and null on the current status.
 */
public record StatusChange(
        Long id,
        long orderId,
        long customerId,
        long restaurantId,
        Long employeeId,
        OrderStatus previousStatus,
        OrderStatus newStatus,
        Instant occurredAt,
        Long secondsInStatus) {

    /** The same order, people, and statuses. The id, the time, and the duration are ignored. */
    public boolean sameFact(StatusChange other) {
        return orderId == other.orderId
                && customerId == other.customerId
                && restaurantId == other.restaurantId
                && Objects.equals(employeeId, other.employeeId)
                && previousStatus == other.previousStatus
                && newStatus == other.newStatus;
    }

    /** Returns a copy with the time spent in {@code newStatus}. */
    public StatusChange withSeconds(Long secondsInStatus) {
        return new StatusChange(
                id,
                orderId,
                customerId,
                restaurantId,
                employeeId,
                previousStatus,
                newStatus,
                occurredAt,
                secondsInStatus);
    }
}
