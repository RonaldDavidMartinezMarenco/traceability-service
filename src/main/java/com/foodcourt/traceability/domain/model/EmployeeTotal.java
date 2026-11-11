package com.foodcourt.traceability.domain.model;

/** Sum of finished-order seconds for one employee. The average is whole seconds. */
public record EmployeeTotal(long employeeId, int completedOrders, long totalSeconds) {

    public EmployeeTotal {
        if (employeeId < 1 || completedOrders < 1 || totalSeconds < 0) {
            throw new IllegalArgumentException("The employee total is not a completed set");
        }
    }

    /** Integer division. A 5 second order and a 6 second order average 5. */
    public long averageSeconds() {
        return totalSeconds / completedOrders;
    }
}
