package com.foodcourt.traceability.infrastructure.output.jpa.entity;

import java.time.Instant;

import com.foodcourt.traceability.domain.model.OrderStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Row of {@code order_status_change}. The duration is not stored. */
@Entity
@Table(name = "order_status_change")
public class OrderStatusChangeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "restaurant_id", nullable = false)
    private Long restaurantId;

    @Column(name = "employee_id")
    private Long employeeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "previous_status", length = 20)
    private OrderStatus previousStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", nullable = false, length = 20)
    private OrderStatus newStatus;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    protected OrderStatusChangeEntity() {
    }

    public OrderStatusChangeEntity(
            Long id,
            Long orderId,
            Long customerId,
            Long restaurantId,
            Long employeeId,
            OrderStatus previousStatus,
            OrderStatus newStatus,
            Instant occurredAt) {
        this.id = id;
        this.orderId = orderId;
        this.customerId = customerId;
        this.restaurantId = restaurantId;
        this.employeeId = employeeId;
        this.previousStatus = previousStatus;
        this.newStatus = newStatus;
        this.occurredAt = occurredAt;
    }

    public Long getId() {
        return id;
    }

    public Long getOrderId() {
        return orderId;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public Long getRestaurantId() {
        return restaurantId;
    }

    public Long getEmployeeId() {
        return employeeId;
    }

    public OrderStatus getPreviousStatus() {
        return previousStatus;
    }

    public OrderStatus getNewStatus() {
        return newStatus;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }
}
