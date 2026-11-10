package com.foodcourt.traceability.infrastructure.output.jpa.mapper;

import com.foodcourt.traceability.domain.model.StatusChange;
import com.foodcourt.traceability.infrastructure.output.jpa.entity.OrderStatusChangeEntity;

/** Copies fields between the JPA row and the domain. It applies no rule. */
public final class OrderStatusChangeJpaMapper {

    public OrderStatusChangeEntity toEntity(StatusChange change) {
        return new OrderStatusChangeEntity(
                change.id(),
                change.orderId(),
                change.customerId(),
                change.restaurantId(),
                change.employeeId(),
                change.previousStatus(),
                change.newStatus(),
                change.occurredAt());
    }

    public StatusChange toDomain(OrderStatusChangeEntity entity) {
        return new StatusChange(
                entity.getId(),
                entity.getOrderId(),
                entity.getCustomerId(),
                entity.getRestaurantId(),
                entity.getEmployeeId(),
                entity.getPreviousStatus(),
                entity.getNewStatus(),
                entity.getOccurredAt(),
                null);
    }
}
