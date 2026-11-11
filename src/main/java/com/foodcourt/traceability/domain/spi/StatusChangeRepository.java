package com.foodcourt.traceability.domain.spi;

import java.util.List;

import com.foodcourt.traceability.domain.model.CompletedInterval;
import com.foodcourt.traceability.domain.model.EmployeeTotal;
import com.foodcourt.traceability.domain.model.PageQuery;
import com.foodcourt.traceability.domain.model.PageResult;
import com.foodcourt.traceability.domain.model.SaveResult;
import com.foodcourt.traceability.domain.model.StatusChange;

/** Persistence port for {@code order_status_change}. Rows come back oldest first. */
public interface StatusChangeRepository {

    List<StatusChange> findByOrderId(long orderId);

    List<StatusChange> findByOrderAndCustomer(long orderId, long customerId);

    /**
     * Finished orders of one restaurant, longest duration first.
     * A finished order starts at {@code IN_PREPARATION} and ends at {@code DELIVERED}.
     */
    PageResult<CompletedInterval> findCompleted(long restaurantId, PageQuery page);

    /** One total per employee who finished at least one order at the restaurant. */
    List<EmployeeTotal> summarizeByEmployee(long restaurantId);

    SaveResult save(StatusChange change);
}
