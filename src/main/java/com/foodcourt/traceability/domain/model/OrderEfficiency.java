package com.foodcourt.traceability.domain.model;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Finished orders of one restaurant and the ranking of employees by average duration.
 * The average is the sum of seconds divided by the number of finished orders.
 * An employee with one slow order does not weigh the same as an employee with many.
 */
public record OrderEfficiency(
        long restaurantId,
        int completedOrders,
        Long averageDurationSeconds,
        PageResult<OrderDuration> orders,
        List<EmployeeRank> employeeRanking) {

    public OrderEfficiency {
        employeeRanking = List.copyOf(employeeRanking);
    }

    /**
     * Builds the report from one page of finished orders and the totals of every finished order.
     * An empty restaurant has a null average and an empty ranking.
     */
    public static OrderEfficiency of(
            long restaurantId,
            PageResult<CompletedInterval> page,
            List<EmployeeTotal> totals) {
        List<EmployeeTotal> safe = List.copyOf(totals);
        Long average = average(safe);
        return new OrderEfficiency(restaurantId, completed(safe), average, mark(page, average), rank(safe));
    }

    private static Long average(List<EmployeeTotal> totals) {
        int count = completed(totals);
        if (count == 0) {
            return null;
        }
        return totalSeconds(totals) / count;
    }

    private static int completed(List<EmployeeTotal> totals) {
        int count = 0;
        for (EmployeeTotal total : totals) {
            count += total.completedOrders();
        }
        return count;
    }

    private static long totalSeconds(List<EmployeeTotal> totals) {
        long seconds = 0;
        for (EmployeeTotal total : totals) {
            seconds += total.totalSeconds();
        }
        return seconds;
    }

    private static PageResult<OrderDuration> mark(PageResult<CompletedInterval> page, Long average) {
        return new PageResult<>(durations(page.items(), average), page.page(), page.size(), page.totalItems());
    }

    private static List<OrderDuration> durations(List<CompletedInterval> intervals, Long average) {
        return intervals.stream().map(interval -> duration(interval, average)).toList();
    }

    private static OrderDuration duration(CompletedInterval interval, Long average) {
        long seconds = interval.durationSeconds();
        return new OrderDuration(
                interval.orderId(),
                interval.employeeId(),
                interval.startedAt(),
                interval.finishedAt(),
                seconds,
                slower(seconds, average));
    }

    /** Equal to the average is not slower. */
    private static boolean slower(long seconds, Long average) {
        return average != null && seconds > average;
    }

    private static List<EmployeeRank> rank(List<EmployeeTotal> totals) {
        List<EmployeeTotal> ordered = new ArrayList<>(totals);
        ordered.sort(Comparator.comparingLong(EmployeeTotal::averageSeconds)
                .thenComparingLong(EmployeeTotal::employeeId));
        return positions(ordered);
    }

    private static List<EmployeeRank> positions(List<EmployeeTotal> ordered) {
        List<EmployeeRank> ranks = new ArrayList<>();
        for (int index = 0; index < ordered.size(); index++) {
            ranks.add(position(index + 1, ordered.get(index)));
        }
        return List.copyOf(ranks);
    }

    private static EmployeeRank position(int rank, EmployeeTotal total) {
        return new EmployeeRank(rank, total.employeeId(), total.completedOrders(), total.averageSeconds());
    }
}
