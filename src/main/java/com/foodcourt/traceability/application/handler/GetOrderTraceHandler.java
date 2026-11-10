package com.foodcourt.traceability.application.handler;

import org.springframework.stereotype.Service;

import com.foodcourt.traceability.domain.api.GetOrderTrace;
import com.foodcourt.traceability.domain.model.OrderTrace;
import com.foodcourt.traceability.domain.spi.CurrentCaller;

/** Entry the controller calls. The rules stay in {@link GetOrderTrace}. */
@Service
public final class GetOrderTraceHandler {

    private final GetOrderTrace getOrderTrace;
    private final CurrentCaller currentCaller;

    public GetOrderTraceHandler(GetOrderTrace getOrderTrace, CurrentCaller currentCaller) {
        this.getOrderTrace = getOrderTrace;
        this.currentCaller = currentCaller;
    }

    public OrderTrace get(long orderId) {
        return getOrderTrace.get(currentCaller.require(), orderId);
    }
}
