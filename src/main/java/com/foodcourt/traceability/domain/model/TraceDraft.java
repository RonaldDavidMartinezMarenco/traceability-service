package com.foodcourt.traceability.domain.model;

/**
 * A status change requested by plaza. The service stamps the time when it accepts the row.
 * {@code employeeId} is null for {@link OrderStatus#PENDING} and {@link OrderStatus#CANCELLED}.
 */
public record TraceDraft(
        long orderId,
        long customerId,
        long restaurantId,
        Long employeeId,
        OrderStatus previousStatus,
        OrderStatus newStatus) {
}
