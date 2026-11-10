package com.foodcourt.traceability.domain.model;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import com.foodcourt.traceability.domain.exception.IllegalStatusTransition;
import com.foodcourt.traceability.domain.exception.InvalidTraceRequest;
import com.foodcourt.traceability.domain.exception.TraceConflict;

/**
 * The status history of one order, oldest first.
 * A new order opens at {@link OrderStatus#PENDING}. From there it is either cancelled or prepared,
 * then made ready, then delivered. {@link OrderStatus#DELIVERED} and {@link OrderStatus#CANCELLED}
 * do not move again. The employee who starts preparation is the only one who can finish it.
 * Repeating the same fact returns the stored row.
 */
public final class OrderTrace {

    private final List<StatusChange> changes;

    private OrderTrace(List<StatusChange> changes) {
        this.changes = changes;
    }

    /** Copies the rows and orders them by time, then by id. */
    public static OrderTrace of(List<StatusChange> changes) {
        List<StatusChange> ordered = new ArrayList<>(changes);
        ordered.sort(Comparator.comparing(StatusChange::occurredAt)
                .thenComparing(StatusChange::id, Comparator.nullsLast(Long::compareTo)));
        return new OrderTrace(List.copyOf(ordered));
    }

    public List<StatusChange> changes() {
        return changes;
    }

    public boolean isEmpty() {
        return changes.isEmpty();
    }

    /**
     * Accepts {@code draft} at {@code occurredAt}, or returns the row already stored for that status.
     * An employee is required only for preparation, ready, and delivered.
     */
    public StatusChange accept(TraceDraft draft, Instant occurredAt) {
        requireEmployeeShape(draft);
        StatusChange existing = find(draft.newStatus());
        if (existing != null) {
            return replay(existing, draft);
        }
        requireNext(draft, occurredAt);
        return changeFrom(draft, occurredAt);
    }

    /** Fills {@code secondsInStatus} on every closed interval. The current status stays null. */
    public OrderTrace withDurations() {
        List<StatusChange> timed = new ArrayList<>();
        for (int index = 0; index < changes.size(); index++) {
            timed.add(withGap(changes.get(index), nextAt(index)));
        }
        return new OrderTrace(List.copyOf(timed));
    }

    private static void requireEmployeeShape(TraceDraft draft) {
        if (kitchen(draft.newStatus())) {
            requireEmployee(draft.employeeId());
            return;
        }
        rejectEmployee(draft.employeeId());
    }

    private static boolean kitchen(OrderStatus status) {
        return status == OrderStatus.IN_PREPARATION || status == OrderStatus.READY || status == OrderStatus.DELIVERED;
    }

    private static void requireEmployee(Long employeeId) {
        if (employeeId == null) {
            throw new InvalidTraceRequest("An employee is required for this status");
        }
    }

    private static void rejectEmployee(Long employeeId) {
        if (employeeId != null) {
            throw new InvalidTraceRequest("This status does not take an employee");
        }
    }

    private StatusChange find(OrderStatus status) {
        for (StatusChange change : changes) {
            if (change.newStatus() == status) {
                return change;
            }
        }
        return null;
    }

    private static StatusChange replay(StatusChange existing, TraceDraft draft) {
        if (!matches(existing, draft)) {
            throw new TraceConflict();
        }
        return existing;
    }

    private static boolean matches(StatusChange existing, TraceDraft draft) {
        return existing.customerId() == draft.customerId()
                && existing.restaurantId() == draft.restaurantId()
                && Objects.equals(existing.employeeId(), draft.employeeId())
                && existing.previousStatus() == draft.previousStatus();
    }

    private void requireNext(TraceDraft draft, Instant occurredAt) {
        requireLegal(draft);
        requireIdentity(draft);
        requireTime(occurredAt);
    }

    private void requireLegal(TraceDraft draft) {
        if (changes.isEmpty()) {
            requireOpening(draft);
            return;
        }
        requireFollows(draft);
    }

    private static void requireOpening(TraceDraft draft) {
        if (draft.previousStatus() != null || draft.newStatus() != OrderStatus.PENDING) {
            throw new IllegalStatusTransition();
        }
    }

    private void requireFollows(TraceDraft draft) {
        StatusChange last = changes.get(changes.size() - 1);
        if (last.newStatus() != draft.previousStatus() || !allowed(draft.previousStatus(), draft.newStatus())) {
            throw new IllegalStatusTransition();
        }
    }

    private static boolean allowed(OrderStatus previous, OrderStatus next) {
        if (previous == OrderStatus.PENDING) {
            return next == OrderStatus.IN_PREPARATION || next == OrderStatus.CANCELLED;
        }
        if (previous == OrderStatus.IN_PREPARATION) {
            return next == OrderStatus.READY;
        }
        if (previous == OrderStatus.READY) {
            return next == OrderStatus.DELIVERED;
        }
        return false;
    }

    private void requireIdentity(TraceDraft draft) {
        if (changes.isEmpty()) {
            return;
        }
        StatusChange first = changes.get(0);
        requireSameCustomer(first, draft);
        requireSameRestaurant(first, draft);
        requireSameEmployee(draft);
    }

    private static void requireSameCustomer(StatusChange first, TraceDraft draft) {
        if (first.customerId() != draft.customerId()) {
            throw new TraceConflict();
        }
    }

    private static void requireSameRestaurant(StatusChange first, TraceDraft draft) {
        if (first.restaurantId() != draft.restaurantId()) {
            throw new TraceConflict();
        }
    }

    private void requireSameEmployee(TraceDraft draft) {
        Long assigned = assignedEmployee();
        if (assigned == null || draft.employeeId() == null) {
            return;
        }
        if (!assigned.equals(draft.employeeId())) {
            throw new TraceConflict();
        }
    }

    private Long assignedEmployee() {
        for (StatusChange change : changes) {
            if (change.newStatus() == OrderStatus.IN_PREPARATION) {
                return change.employeeId();
            }
        }
        return null;
    }

    private void requireTime(Instant occurredAt) {
        if (changes.isEmpty() || !occurredAt.isBefore(lastAt())) {
            return;
        }
        throw new IllegalStatusTransition();
    }

    private Instant lastAt() {
        return changes.get(changes.size() - 1).occurredAt();
    }

    private static StatusChange changeFrom(TraceDraft draft, Instant occurredAt) {
        return new StatusChange(
                null,
                draft.orderId(),
                draft.customerId(),
                draft.restaurantId(),
                draft.employeeId(),
                draft.previousStatus(),
                draft.newStatus(),
                occurredAt,
                null);
    }

    private Instant nextAt(int index) {
        if (index + 1 >= changes.size()) {
            return null;
        }
        return changes.get(index + 1).occurredAt();
    }

    private static StatusChange withGap(StatusChange change, Instant next) {
        if (next == null) {
            return change.withSeconds(null);
        }
        return change.withSeconds(Duration.between(change.occurredAt(), next).getSeconds());
    }
}
