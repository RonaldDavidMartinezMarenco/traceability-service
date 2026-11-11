package com.foodcourt.traceability.domain.api;

import com.foodcourt.traceability.domain.model.Caller;
import com.foodcourt.traceability.domain.model.OrderEfficiency;
import com.foodcourt.traceability.domain.model.PageQuery;

/** Reads finished-order durations and the employee ranking for a restaurant the caller owns. */
public interface GetOrderEfficiency {

    OrderEfficiency get(Caller caller, long restaurantId, PageQuery page);
}
