package com.foodcourt.traceability.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

class OrderEfficiencyTest {

    private static final Instant START = Instant.parse("2026-11-10T17:05:00Z");

    @Test
    void theRestaurantAverageIsTheSumDividedByEveryFinishedOrder() {
        PageResult<CompletedInterval> page = new PageResult<>(List.of(interval(4L, 7L, 6)), 0, 10, 2);
        List<EmployeeTotal> totals = List.of(new EmployeeTotal(8L, 1, 5), new EmployeeTotal(7L, 1, 6));

        OrderEfficiency report = OrderEfficiency.of(1L, page, totals);

        assertThat(report.completedOrders()).isEqualTo(2);
        assertThat(report.averageDurationSeconds()).isEqualTo(5L);
        assertThat(report.orders().totalPages()).isEqualTo(1);
        assertThat(report.orders().items()).singleElement().satisfies(order -> {
            assertThat(order.orderId()).isEqualTo(4L);
            assertThat(order.durationSeconds()).isEqualTo(6L);
            assertThat(order.slowerThanAverage()).isTrue();
        });
        assertThat(report.employeeRanking()).containsExactly(
                new EmployeeRank(1, 8L, 1, 5L),
                new EmployeeRank(2, 7L, 1, 6L));
    }

    @Test
    void twoOrdersOfOneEmployeeDoNotWeighTheSameAsOneOrderOfAnother() {
        PageResult<CompletedInterval> page = new PageResult<>(List.of(), 0, 10, 3);
        List<EmployeeTotal> totals = List.of(new EmployeeTotal(7L, 2, 11), new EmployeeTotal(8L, 1, 100));

        OrderEfficiency report = OrderEfficiency.of(1L, page, totals);

        assertThat(report.completedOrders()).isEqualTo(3);
        assertThat(report.averageDurationSeconds()).isEqualTo(37L);
        assertThat(report.employeeRanking()).containsExactly(
                new EmployeeRank(1, 7L, 2, 5L),
                new EmployeeRank(2, 8L, 1, 100L));
    }

    @Test
    void aSingleOrderIsNotSlowerThanItsOwnAverage() {
        OrderEfficiency report = OrderEfficiency.of(
                1L,
                new PageResult<>(List.of(interval(1L, 5L, 10)), 0, 10, 1),
                List.of(new EmployeeTotal(5L, 1, 10)));

        assertThat(report.averageDurationSeconds()).isEqualTo(10L);
        assertThat(report.orders().items().get(0).slowerThanAverage()).isFalse();
        assertThat(report.employeeRanking()).containsExactly(new EmployeeRank(1, 5L, 1, 10L));
    }

    @Test
    void equalAveragesAreOrderedByEmployeeId() {
        OrderEfficiency report = OrderEfficiency.of(
                1L,
                new PageResult<>(List.of(), 0, 10, 2),
                List.of(new EmployeeTotal(9L, 1, 10), new EmployeeTotal(3L, 1, 10)));

        assertThat(report.employeeRanking()).extracting(EmployeeRank::employeeId).containsExactly(3L, 9L);
        assertThat(report.employeeRanking()).extracting(EmployeeRank::rank).containsExactly(1, 2);
    }

    @Test
    void anEmptyRestaurantHasNoAverageAndNoRanking() {
        OrderEfficiency report = OrderEfficiency.of(1L, new PageResult<>(List.of(), 0, 10, 0), List.of());

        assertThat(report.completedOrders()).isZero();
        assertThat(report.averageDurationSeconds()).isNull();
        assertThat(report.orders().items()).isEmpty();
        assertThat(report.orders().totalPages()).isZero();
        assertThat(report.employeeRanking()).isEmpty();
    }

    @Test
    void anEmployeeTotalRequiresAtLeastOneFinishedOrder() {
        assertThatThrownBy(() -> new EmployeeTotal(5L, 0, 0)).isInstanceOf(IllegalArgumentException.class);
    }

    private static CompletedInterval interval(long orderId, long employeeId, long seconds) {
        return new CompletedInterval(orderId, 1L, employeeId, START, START.plusSeconds(seconds));
    }
}
