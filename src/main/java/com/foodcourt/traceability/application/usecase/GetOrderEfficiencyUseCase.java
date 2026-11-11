package com.foodcourt.traceability.application.usecase;

import org.springframework.stereotype.Service;

import com.foodcourt.traceability.domain.api.GetOrderEfficiency;
import com.foodcourt.traceability.domain.model.Caller;
import com.foodcourt.traceability.domain.model.OrderEfficiency;
import com.foodcourt.traceability.domain.model.PageQuery;
import com.foodcourt.traceability.domain.spi.AccountDirectory;
import com.foodcourt.traceability.domain.spi.RestaurantDirectory;
import com.foodcourt.traceability.domain.spi.StatusChangeRepository;

/**
 * Returns the finished-order durations and the employee ranking of a restaurant the caller owns.
 * Orders that are still open, and cancelled orders, stay out of both results.
 */
@Service
public final class GetOrderEfficiencyUseCase implements GetOrderEfficiency {

    private final StatusChangeRepository changes;
    private final RestaurantOwnerRule owners;

    public GetOrderEfficiencyUseCase(
            StatusChangeRepository changes,
            AccountDirectory accounts,
            RestaurantDirectory restaurants) {
        this.changes = changes;
        this.owners = new RestaurantOwnerRule(accounts, restaurants);
    }

    @Override
    public OrderEfficiency get(Caller caller, long restaurantId, PageQuery page) {
        owners.require(caller, restaurantId);
        return OrderEfficiency.of(
                restaurantId,
                changes.findCompleted(restaurantId, page),
                changes.summarizeByEmployee(restaurantId));
    }
}
