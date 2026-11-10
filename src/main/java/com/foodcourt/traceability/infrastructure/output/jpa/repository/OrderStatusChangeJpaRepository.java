package com.foodcourt.traceability.infrastructure.output.jpa.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.foodcourt.traceability.domain.model.OrderStatus;
import com.foodcourt.traceability.infrastructure.output.jpa.entity.OrderStatusChangeEntity;

/** Spring Data access to {@code order_status_change}. Used only by the JPA adapter. */
public interface OrderStatusChangeJpaRepository extends JpaRepository<OrderStatusChangeEntity, Long> {

    List<OrderStatusChangeEntity> findByOrderIdOrderByOccurredAtAscIdAsc(long orderId);

    List<OrderStatusChangeEntity> findByOrderIdAndCustomerIdOrderByOccurredAtAscIdAsc(long orderId, long customerId);

    Optional<OrderStatusChangeEntity> findByOrderIdAndNewStatus(long orderId, OrderStatus newStatus);
}
