package com.foodcourt.traceability.application.usecase;

import org.springframework.stereotype.Service;

import com.foodcourt.traceability.domain.api.GetOrderTrace;
import com.foodcourt.traceability.domain.exception.OrderTraceNotFound;
import com.foodcourt.traceability.domain.model.Caller;
import com.foodcourt.traceability.domain.model.OrderTrace;
import com.foodcourt.traceability.domain.spi.AccountDirectory;
import com.foodcourt.traceability.domain.spi.StatusChangeRepository;

/**
 * Returns the status history of one order for the customer who owns it.
 * Another customer's order looks the same as an order that was never recorded.
 */
@Service
public final class GetOrderTraceUseCase implements GetOrderTrace {

    private final StatusChangeRepository changes;
    private final TraceActorRule actors;

    public GetOrderTraceUseCase(StatusChangeRepository changes, AccountDirectory accounts) {
        this.changes = changes;
        this.actors = new TraceActorRule(accounts);
    }

    @Override
    public OrderTrace get(Caller caller, long orderId) {
        actors.requireReader(caller);
        OrderTrace trace = OrderTrace.of(changes.findByOrderAndCustomer(orderId, caller.id()));
        if (trace.isEmpty()) {
            throw new OrderTraceNotFound();
        }
        return trace.withDurations();
    }
}
