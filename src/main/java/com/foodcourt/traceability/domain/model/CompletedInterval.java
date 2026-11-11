package com.foodcourt.traceability.domain.model;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/**
 * A finished order. The interval starts at {@link OrderStatus#IN_PREPARATION}
 * and ends at {@link OrderStatus#DELIVERED} for the same employee and restaurant.
 * Time spent in {@link OrderStatus#PENDING} is not part of the interval.
 */
public record CompletedInterval(
        long orderId,
        long restaurantId,
        long employeeId,
        Instant startedAt,
        Instant finishedAt) {

    public CompletedInterval {
        if (!consistent(orderId, restaurantId, employeeId, startedAt, finishedAt)) {
            throw new IllegalArgumentException("The completed interval is not a finished order");
        }
    }

    /**
     * The preparation row and the delivery row of one order.
     * Empty when the pair is not a finished order for one employee.
     */
    public static Optional<CompletedInterval> between(StatusChange start, StatusChange finish) {
        if (start == null || finish == null || !acceptable(start, finish)) {
            return Optional.empty();
        }
        return Optional.of(new CompletedInterval(
                start.orderId(),
                start.restaurantId(),
                start.employeeId(),
                start.occurredAt(),
                finish.occurredAt()));
    }

    /** Whole seconds from preparation to delivery. Zero is a finished order. */
    public long durationSeconds() {
        return Duration.between(startedAt, finishedAt).getSeconds();
    }

    private static boolean acceptable(StatusChange start, StatusChange finish) {
        return preparation(start) && delivered(finish) && sameOrder(start, finish) && aligned(start, finish);
    }

    private static boolean preparation(StatusChange start) {
        return start.newStatus() == OrderStatus.IN_PREPARATION;
    }

    private static boolean delivered(StatusChange finish) {
        return finish.newStatus() == OrderStatus.DELIVERED;
    }

    private static boolean sameOrder(StatusChange start, StatusChange finish) {
        return start.orderId() == finish.orderId();
    }

    private static boolean aligned(StatusChange start, StatusChange finish) {
        return sameRestaurant(start, finish) && sameEmployee(start, finish) && inOrder(start, finish);
    }

    private static boolean sameRestaurant(StatusChange start, StatusChange finish) {
        return start.restaurantId() == finish.restaurantId();
    }

    private static boolean sameEmployee(StatusChange start, StatusChange finish) {
        return start.employeeId() != null && start.employeeId().equals(finish.employeeId());
    }

    private static boolean inOrder(StatusChange start, StatusChange finish) {
        return start.occurredAt() != null
                && finish.occurredAt() != null
                && !finish.occurredAt().isBefore(start.occurredAt());
    }

    private static boolean consistent(
            long orderId,
            long restaurantId,
            long employeeId,
            Instant startedAt,
            Instant finishedAt) {
        return orderId > 0
                && restaurantId > 0
                && employeeId > 0
                && startedAt != null
                && finishedAt != null
                && !finishedAt.isBefore(startedAt);
    }
}
