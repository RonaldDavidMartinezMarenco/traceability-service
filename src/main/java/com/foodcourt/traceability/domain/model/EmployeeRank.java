package com.foodcourt.traceability.domain.model;

/** One place in the employee ranking. Rank 1 is the lowest average. */
public record EmployeeRank(int rank, long employeeId, int completedOrders, long averageDurationSeconds) {
}
