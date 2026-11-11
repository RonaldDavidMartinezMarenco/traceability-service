package com.foodcourt.traceability.infrastructure.input.rest.dto;

import java.util.List;

/**
 * Finished orders of one restaurant and the ranking of its employees.
 * {@code averageDurationSeconds} is null when the restaurant has no finished order.
 */
public record OrderEfficiencyResponse(
        long restaurantId,
        int completedOrders,
        Long averageDurationSeconds,
        PageResponse<OrderDurationResponse> orders,
        List<EmployeeRankResponse> employeeRanking) {
}
