package com.foodcourt.traceability.infrastructure.input.rest.dto;

/** One place in the employee ranking. Rank 1 is the lowest average. */
public record EmployeeRankResponse(
        int rank,
        long employeeId,
        int completedOrders,
        long averageDurationSeconds) {
}
