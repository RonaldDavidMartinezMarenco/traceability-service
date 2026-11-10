package com.foodcourt.traceability.infrastructure.input.rest.mapper;

import java.util.List;

import com.foodcourt.traceability.domain.model.OrderStatus;
import com.foodcourt.traceability.domain.model.OrderTrace;
import com.foodcourt.traceability.domain.model.StatusChange;
import com.foodcourt.traceability.domain.model.TraceDraft;
import com.foodcourt.traceability.infrastructure.input.rest.dto.OrderTraceResponse;
import com.foodcourt.traceability.infrastructure.input.rest.dto.RecordStatusChangeRequest;
import com.foodcourt.traceability.infrastructure.input.rest.dto.StatusChangeResponse;
import com.foodcourt.traceability.infrastructure.input.rest.dto.TraceEntryResponse;

/** Copies fields between the HTTP records and the domain. It applies no rule. */
public final class OrderTraceRestMapper {

    public TraceDraft toDraft(RecordStatusChangeRequest request) {
        return new TraceDraft(
                request.orderId(),
                request.customerId(),
                request.restaurantId(),
                request.employeeId(),
                OrderStatus.parseOptional(request.previousStatus()),
                OrderStatus.parseExact(request.newStatus()));
    }

    public StatusChangeResponse toResponse(StatusChange change) {
        return new StatusChangeResponse(
                change.orderId(),
                change.customerId(),
                change.restaurantId(),
                change.previousStatus(),
                change.newStatus(),
                change.occurredAt());
    }

    public OrderTraceResponse toResponse(OrderTrace trace) {
        StatusChange first = trace.changes().get(0);
        return new OrderTraceResponse(first.orderId(), first.customerId(), first.restaurantId(), entries(trace));
    }

    private List<TraceEntryResponse> entries(OrderTrace trace) {
        return trace.changes().stream().map(this::entry).toList();
    }

    private TraceEntryResponse entry(StatusChange change) {
        return new TraceEntryResponse(
                change.previousStatus(),
                change.newStatus(),
                change.occurredAt(),
                change.secondsInStatus());
    }
}
