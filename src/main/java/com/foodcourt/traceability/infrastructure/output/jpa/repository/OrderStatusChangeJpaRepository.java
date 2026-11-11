package com.foodcourt.traceability.infrastructure.output.jpa.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.foodcourt.traceability.domain.model.OrderStatus;
import com.foodcourt.traceability.infrastructure.output.jpa.entity.OrderStatusChangeEntity;

/** Spring Data access to {@code order_status_change}. Used only by the JPA adapter. */
public interface OrderStatusChangeJpaRepository extends JpaRepository<OrderStatusChangeEntity, Long> {

    String COMPLETED_FROM = """
            FROM order_status_change start_row
            JOIN order_status_change finish_row
              ON finish_row.order_id = start_row.order_id
             AND finish_row.new_status = 'DELIVERED'
             AND finish_row.restaurant_id = start_row.restaurant_id
             AND finish_row.employee_id = start_row.employee_id
            WHERE start_row.restaurant_id = :restaurantId
              AND start_row.new_status = 'IN_PREPARATION'
              AND start_row.employee_id IS NOT NULL
              AND finish_row.occurred_at >= start_row.occurred_at
            """;

    List<OrderStatusChangeEntity> findByOrderIdOrderByOccurredAtAscIdAsc(long orderId);

    List<OrderStatusChangeEntity> findByOrderIdAndCustomerIdOrderByOccurredAtAscIdAsc(long orderId, long customerId);

    Optional<OrderStatusChangeEntity> findByOrderIdAndNewStatus(long orderId, OrderStatus newStatus);

    @Query(
            value = """
                    SELECT start_row.order_id,
                           start_row.restaurant_id,
                           start_row.employee_id,
                           start_row.occurred_at,
                           finish_row.occurred_at
                    """ + COMPLETED_FROM + """
                    ORDER BY (finish_row.occurred_at - start_row.occurred_at) DESC,
                             start_row.order_id ASC
                    """,
            countQuery = "SELECT COUNT(*) " + COMPLETED_FROM,
            nativeQuery = true)
    Page<Object[]> findCompleted(@Param("restaurantId") long restaurantId, Pageable pageable);

    @Query(
            value = """
                    SELECT start_row.employee_id,
                           COUNT(*) AS completed_orders,
                           SUM(FLOOR(EXTRACT(EPOCH FROM (finish_row.occurred_at - start_row.occurred_at))))
                               AS total_seconds
                    """ + COMPLETED_FROM + """
                    GROUP BY start_row.employee_id
                    """,
            nativeQuery = true)
    List<Object[]> summarizeByEmployee(@Param("restaurantId") long restaurantId);
}
