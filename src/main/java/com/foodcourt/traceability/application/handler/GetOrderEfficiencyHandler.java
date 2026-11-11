package com.foodcourt.traceability.application.handler;

import org.springframework.stereotype.Service;

import com.foodcourt.traceability.domain.api.GetOrderEfficiency;
import com.foodcourt.traceability.domain.model.OrderEfficiency;
import com.foodcourt.traceability.domain.model.PageQuery;
import com.foodcourt.traceability.domain.spi.CurrentCaller;

/** Entry the controller calls. The rules stay in {@link GetOrderEfficiency}. */
@Service
public final class GetOrderEfficiencyHandler {

    private final GetOrderEfficiency getOrderEfficiency;
    private final CurrentCaller currentCaller;

    public GetOrderEfficiencyHandler(GetOrderEfficiency getOrderEfficiency, CurrentCaller currentCaller) {
        this.getOrderEfficiency = getOrderEfficiency;
        this.currentCaller = currentCaller;
    }

    public OrderEfficiency get(long restaurantId, PageQuery page) {
        return getOrderEfficiency.get(currentCaller.require(), restaurantId, page);
    }
}
