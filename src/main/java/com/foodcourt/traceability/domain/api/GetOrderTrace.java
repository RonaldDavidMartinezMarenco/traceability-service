package com.foodcourt.traceability.domain.api;

import com.foodcourt.traceability.domain.model.Caller;
import com.foodcourt.traceability.domain.model.OrderTrace;

/** Reads the status history of one order for the customer who placed it. */
public interface GetOrderTrace {

    OrderTrace get(Caller caller, long orderId);
}
