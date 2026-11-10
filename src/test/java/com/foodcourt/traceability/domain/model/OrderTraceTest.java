package com.foodcourt.traceability.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.foodcourt.traceability.domain.exception.IllegalStatusTransition;
import com.foodcourt.traceability.domain.exception.InvalidTraceRequest;
import com.foodcourt.traceability.domain.exception.TraceConflict;

class OrderTraceTest {

    private static final Instant OPENED = Instant.parse("2026-11-10T17:00:00Z");
    private static final Instant PREPARED = OPENED.plusSeconds(300);
    private static final Instant READY = OPENED.plusSeconds(900);
    private static final Instant CLOSED = OPENED.plusSeconds(1080);

    @Test
    void acceptOpensAnOrderAtPendingWithoutAnEmployee() {
        StatusChange stored = OrderTrace.of(List.of()).accept(draft(null, null, OrderStatus.PENDING), OPENED);

        assertThat(stored.id()).isNull();
        assertThat(stored.previousStatus()).isNull();
        assertThat(stored.newStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(stored.employeeId()).isNull();
        assertThat(stored.occurredAt()).isEqualTo(OPENED);
        assertThat(stored.secondsInStatus()).isNull();
    }

    @Test
    void acceptFollowsTheLegalPathThroughDelivery() {
        OrderTrace trace = history(pending(), preparation(), ready());

        StatusChange delivered = trace.accept(draft(4L, OrderStatus.READY, OrderStatus.DELIVERED), CLOSED);

        assertThat(delivered.newStatus()).isEqualTo(OrderStatus.DELIVERED);
        assertThat(delivered.employeeId()).isEqualTo(4L);
        assertThat(delivered.occurredAt()).isEqualTo(CLOSED);
    }

    @Test
    void acceptCancelsAPendingOrder() {
        StatusChange cancelled = OrderTrace.of(List.of(pending()))
                .accept(draft(null, OrderStatus.PENDING, OrderStatus.CANCELLED), PREPARED);

        assertThat(cancelled.newStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(cancelled.employeeId()).isNull();
    }

    @Test
    void acceptReturnsTheStoredRowWhenTheSameFactIsRepeated() {
        StatusChange again = OrderTrace.of(List.of(pending()))
                .accept(draft(null, null, OrderStatus.PENDING), CLOSED);

        assertThat(again).isEqualTo(pending());
        assertThat(again.occurredAt()).isEqualTo(OPENED);
    }

    @Test
    void withDurationsClosesEveryIntervalExceptTheCurrentStatus() {
        OrderTrace timed = history(pending(), preparation(), ready(), delivered()).withDurations();

        assertThat(timed.changes()).extracting(StatusChange::secondsInStatus).containsExactly(300L, 600L, 180L, null);
    }

    @Test
    void withDurationsLeavesASinglePendingRowOpen() {
        OrderTrace timed = OrderTrace.of(List.of(pending())).withDurations();

        assertThat(timed.changes().get(0).secondsInStatus()).isNull();
    }

    @Test
    void withDurationsClosesThePendingIntervalOfACancelledOrder() {
        StatusChange cancelled = change(2L, null, OrderStatus.PENDING, OrderStatus.CANCELLED, PREPARED);
        OrderTrace timed = OrderTrace.of(List.of(cancelled, pending())).withDurations();

        assertThat(timed.changes()).extracting(StatusChange::newStatus)
                .containsExactly(OrderStatus.PENDING, OrderStatus.CANCELLED);
        assertThat(timed.changes()).extracting(StatusChange::secondsInStatus).containsExactly(300L, null);
    }

    @Test
    void acceptRejectsAnEmployeeOnTheOpeningAndOnACancellation() {
        assertThatThrownBy(() -> OrderTrace.of(List.of()).accept(draft(4L, null, OrderStatus.PENDING), OPENED))
                .isInstanceOf(InvalidTraceRequest.class)
                .hasMessage("This status does not take an employee");
        assertThatThrownBy(() -> OrderTrace.of(List.of(pending()))
                .accept(draft(4L, OrderStatus.PENDING, OrderStatus.CANCELLED), PREPARED))
                .isInstanceOf(InvalidTraceRequest.class)
                .hasMessage("This status does not take an employee");
    }

    @Test
    void acceptRejectsAKitchenStatusWithoutAnEmployee() {
        assertThatThrownBy(() -> OrderTrace.of(List.of(pending()))
                .accept(draft(null, OrderStatus.PENDING, OrderStatus.IN_PREPARATION), PREPARED))
                .isInstanceOf(InvalidTraceRequest.class)
                .hasMessage("An employee is required for this status");
    }

    @Test
    void acceptRejectsAnOrderThatDoesNotOpenAtPending() {
        assertThatThrownBy(() -> OrderTrace.of(List.of())
                .accept(draft(4L, null, OrderStatus.IN_PREPARATION), OPENED))
                .isInstanceOf(IllegalStatusTransition.class);
        assertThatThrownBy(() -> OrderTrace.of(List.of())
                .accept(draft(null, OrderStatus.PENDING, OrderStatus.PENDING), OPENED))
                .isInstanceOf(IllegalStatusTransition.class);
    }

    @Test
    void acceptRejectsASkippedOrBackwardStatus() {
        OrderTrace pendingOnly = OrderTrace.of(List.of(pending()));
        assertThatThrownBy(() -> pendingOnly.accept(draft(4L, OrderStatus.PENDING, OrderStatus.READY), PREPARED))
                .isInstanceOf(IllegalStatusTransition.class);
        assertThatThrownBy(() -> pendingOnly.accept(draft(4L, OrderStatus.PENDING, OrderStatus.DELIVERED), PREPARED))
                .isInstanceOf(IllegalStatusTransition.class);

        OrderTrace preparing = history(pending(), preparation());
        assertThatThrownBy(() -> preparing.accept(draft(null, OrderStatus.IN_PREPARATION, OrderStatus.CANCELLED), READY))
                .isInstanceOf(IllegalStatusTransition.class);
        assertThatThrownBy(() -> preparing.accept(draft(4L, OrderStatus.IN_PREPARATION, OrderStatus.DELIVERED), READY))
                .isInstanceOf(IllegalStatusTransition.class);

        OrderTrace readyOrder = history(pending(), preparation(), ready());
        assertThatThrownBy(() -> readyOrder.accept(draft(null, OrderStatus.READY, OrderStatus.CANCELLED), CLOSED))
                .isInstanceOf(IllegalStatusTransition.class);
        assertThatThrownBy(() -> readyOrder.accept(draft(4L, OrderStatus.READY, OrderStatus.IN_PREPARATION), CLOSED))
                .isInstanceOf(TraceConflict.class);
    }

    @Test
    void acceptRejectsLeavingATerminalStatus() {
        OrderTrace delivered = history(pending(), preparation(), ready(), delivered());
        assertThatThrownBy(() -> delivered.accept(draft(null, OrderStatus.DELIVERED, OrderStatus.CANCELLED), CLOSED.plusSeconds(1)))
                .isInstanceOf(IllegalStatusTransition.class);

        OrderTrace cancelled = OrderTrace.of(List.of(
                pending(), change(2L, null, OrderStatus.PENDING, OrderStatus.CANCELLED, PREPARED)));
        assertThatThrownBy(() -> cancelled.accept(draft(null, OrderStatus.CANCELLED, OrderStatus.PENDING), READY))
                .isInstanceOf(TraceConflict.class);
    }

    @Test
    void acceptRejectsARepeatedStatusWithADifferentIdentity() {
        assertThatThrownBy(() -> OrderTrace.of(List.of(pending()))
                .accept(new TraceDraft(1L, 8L, 1L, null, null, OrderStatus.PENDING), CLOSED))
                .isInstanceOf(TraceConflict.class);
    }

    @Test
    void acceptRejectsALaterRowForAnotherCustomerOrRestaurant() {
        OrderTrace trace = OrderTrace.of(List.of(pending()));

        assertThatThrownBy(() -> trace.accept(new TraceDraft(1L, 8L, 1L, 4L, OrderStatus.PENDING, OrderStatus.IN_PREPARATION), PREPARED))
                .isInstanceOf(TraceConflict.class);
        assertThatThrownBy(() -> trace.accept(new TraceDraft(1L, 9L, 2L, 4L, OrderStatus.PENDING, OrderStatus.IN_PREPARATION), PREPARED))
                .isInstanceOf(TraceConflict.class);
    }

    @Test
    void acceptRejectsADifferentEmployeeAfterPreparationStarts() {
        assertThatThrownBy(() -> history(pending(), preparation())
                .accept(draft(6L, OrderStatus.IN_PREPARATION, OrderStatus.READY), READY))
                .isInstanceOf(TraceConflict.class);
    }

    @Test
    void acceptRejectsAChangeThatHappensBeforeThePreviousOne() {
        assertThatThrownBy(() -> OrderTrace.of(List.of(pending()))
                .accept(draft(4L, OrderStatus.PENDING, OrderStatus.IN_PREPARATION), OPENED.minusSeconds(1)))
                .isInstanceOf(IllegalStatusTransition.class);
    }

    @Test
    void parseExactRejectsAMissingOrUnknownStatus() {
        assertThatThrownBy(() -> OrderStatus.parseExact(null))
                .isInstanceOf(InvalidTraceRequest.class)
                .hasMessage("The status is required");
        assertThatThrownBy(() -> OrderStatus.parseExact("pending"))
                .isInstanceOf(InvalidTraceRequest.class)
                .hasMessage("The status is unknown");
        assertThat(OrderStatus.parseOptional(null)).isNull();
        assertThat(OrderStatus.parseExact("READY")).isEqualTo(OrderStatus.READY);
    }

    private static TraceDraft draft(Long employeeId, OrderStatus previous, OrderStatus next) {
        return new TraceDraft(1L, 9L, 1L, employeeId, previous, next);
    }

    private static OrderTrace history(StatusChange... changes) {
        return OrderTrace.of(List.of(changes));
    }

    private static StatusChange pending() {
        return change(1L, null, null, OrderStatus.PENDING, OPENED);
    }

    private static StatusChange preparation() {
        return change(2L, 4L, OrderStatus.PENDING, OrderStatus.IN_PREPARATION, PREPARED);
    }

    private static StatusChange ready() {
        return change(3L, 4L, OrderStatus.IN_PREPARATION, OrderStatus.READY, READY);
    }

    private static StatusChange delivered() {
        return change(4L, 4L, OrderStatus.READY, OrderStatus.DELIVERED, CLOSED);
    }

    private static StatusChange change(
            Long id,
            Long employeeId,
            OrderStatus previous,
            OrderStatus next,
            Instant occurredAt) {
        return new StatusChange(id, 1L, 9L, 1L, employeeId, previous, next, occurredAt, null);
    }
}
