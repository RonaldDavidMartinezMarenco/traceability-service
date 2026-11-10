package com.foodcourt.traceability.domain.spi;

import java.util.List;

import com.foodcourt.traceability.domain.model.SaveResult;
import com.foodcourt.traceability.domain.model.StatusChange;

/** Persistence port for {@code order_status_change}. Rows come back oldest first. */
public interface StatusChangeRepository {

    List<StatusChange> findByOrderId(long orderId);

    List<StatusChange> findByOrderAndCustomer(long orderId, long customerId);

    SaveResult save(StatusChange change);
}
