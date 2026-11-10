package com.foodcourt.traceability.infrastructure.input.rest.dto;

import com.foodcourt.traceability.domain.model.OrderStatus;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

/** Body of {@code POST /order-status-changes}. The service stamps the time. */
public record RecordStatusChangeRequest(
        @NotNull @Positive Long orderId,
        @NotNull @Positive Long customerId,
        @NotNull @Positive Long restaurantId,
        @Positive Long employeeId,
        @Pattern(regexp = "PENDING|IN_PREPARATION|READY|DELIVERED|CANCELLED") String previousStatus,
        @NotNull @Pattern(regexp = "PENDING|IN_PREPARATION|READY|DELIVERED|CANCELLED") String newStatus) {
}
