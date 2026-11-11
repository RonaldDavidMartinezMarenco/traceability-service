package com.foodcourt.traceability.infrastructure.output.jpa.projection;

import com.foodcourt.traceability.domain.model.EmployeeTotal;

/**
 * One group of the employee summary. Columns are employee id, finished orders, and total seconds.
 */
public record EmployeeTotalRow(long employeeId, int completedOrders, long totalSeconds) {

    public static EmployeeTotalRow from(Object[] row) {
        return new EmployeeTotalRow(number(row[0]), count(row[1]), number(row[2]));
    }

    public EmployeeTotal toTotal() {
        return new EmployeeTotal(employeeId, completedOrders, totalSeconds);
    }

    private static long number(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        throw new IllegalStateException("The efficiency query returned an unexpected number");
    }

    private static int count(Object value) {
        long count = number(value);
        if (count < 1 || count > Integer.MAX_VALUE) {
            throw new IllegalStateException("The completed order count is out of range");
        }
        return (int) count;
    }
}
