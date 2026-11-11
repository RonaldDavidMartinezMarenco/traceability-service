package com.foodcourt.traceability.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class CompletedIntervalTest {

    private static final Instant START = Instant.parse("2026-11-10T17:05:00Z");
    private static final Instant END = Instant.parse("2026-11-10T17:15:00Z");

    @Test
    void durationStartsAtPreparationAndEndsAtDelivery() {
        Optional<CompletedInterval> interval = CompletedInterval.between(
                row(1L, 1L, 5L, OrderStatus.IN_PREPARATION, START),
                row(1L, 1L, 5L, OrderStatus.DELIVERED, END));

        assertThat(interval).isPresent();
        assertThat(interval.orElseThrow().durationSeconds()).isEqualTo(600L);
        assertThat(interval.orElseThrow().employeeId()).isEqualTo(5L);
    }

    @Test
    void aDeliveryAtTheSameInstantLastsZeroSeconds() {
        Optional<CompletedInterval> interval = CompletedInterval.between(
                row(1L, 1L, 5L, OrderStatus.IN_PREPARATION, START),
                row(1L, 1L, 5L, OrderStatus.DELIVERED, START));

        assertThat(interval).isPresent();
        assertThat(interval.orElseThrow().durationSeconds()).isZero();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("unfinished")
    void betweenIsEmptyWhenTheOrderDoesNotFinish(String name, StatusChange start, StatusChange finish) {
        assertThat(CompletedInterval.between(start, finish)).as(name).isEmpty();
    }

    @Test
    void betweenIsEmptyWhenTheRowsAreMissingOrOutOfOrder() {
        StatusChange preparation = row(1L, 1L, 5L, OrderStatus.IN_PREPARATION, START);
        StatusChange delivery = row(1L, 1L, 5L, OrderStatus.DELIVERED, END);

        assertThat(CompletedInterval.between(null, delivery)).isEmpty();
        assertThat(CompletedInterval.between(preparation, null)).isEmpty();
        assertThat(CompletedInterval.between(
                row(1L, 1L, 5L, OrderStatus.IN_PREPARATION, END),
                row(1L, 1L, 5L, OrderStatus.DELIVERED, START))).isEmpty();
        assertThat(CompletedInterval.between(
                row(1L, 1L, 5L, OrderStatus.IN_PREPARATION, null),
                delivery)).isEmpty();
    }

    @Test
    void betweenIsEmptyWhenThePeopleOrTheRestaurantDoNotMatch() {
        StatusChange delivery = row(1L, 1L, 5L, OrderStatus.DELIVERED, END);

        assertThat(CompletedInterval.between(row(1L, 1L, 6L, OrderStatus.IN_PREPARATION, START), delivery)).isEmpty();
        assertThat(CompletedInterval.between(row(1L, 2L, 5L, OrderStatus.IN_PREPARATION, START), delivery)).isEmpty();
        assertThat(CompletedInterval.between(row(9L, 1L, 5L, OrderStatus.IN_PREPARATION, START), delivery)).isEmpty();
        assertThat(CompletedInterval.between(row(1L, 1L, null, OrderStatus.IN_PREPARATION, START), delivery)).isEmpty();
    }

    @Test
    void constructorRejectsAnIntervalThatIsNotAFinishedOrder() {
        assertThatThrownBy(() -> new CompletedInterval(0L, 1L, 5L, START, END))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static Stream<Arguments> unfinished() {
        return Stream.of(
                Arguments.of("pending has not started", pending(), delivered()),
                Arguments.of("cancelled has no finish", cancelled(), delivered()),
                Arguments.of("preparation is still open", preparation(), ready()),
                Arguments.of("cancellation is not a delivery", preparation(), cancelled()),
                Arguments.of("delivery without preparation", ready(), delivered()));
    }

    private static StatusChange pending() {
        return row(1L, 1L, null, OrderStatus.PENDING, START.minusSeconds(300));
    }

    private static StatusChange preparation() {
        return row(1L, 1L, 5L, OrderStatus.IN_PREPARATION, START);
    }

    private static StatusChange ready() {
        return row(1L, 1L, 5L, OrderStatus.READY, START.plusSeconds(200));
    }

    private static StatusChange delivered() {
        return row(1L, 1L, 5L, OrderStatus.DELIVERED, END);
    }

    private static StatusChange cancelled() {
        return row(1L, 1L, null, OrderStatus.CANCELLED, START);
    }

    private static StatusChange row(long orderId, long restaurantId, Long employeeId, OrderStatus status, Instant at) {
        return new StatusChange(1L, orderId, 9L, restaurantId, employeeId, null, status, at, null);
    }
}
