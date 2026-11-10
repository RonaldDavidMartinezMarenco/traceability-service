package com.foodcourt.traceability.infrastructure.output.jpa.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;

import com.foodcourt.traceability.domain.exception.TraceConflict;
import com.foodcourt.traceability.domain.model.OrderStatus;
import com.foodcourt.traceability.domain.model.SaveResult;
import com.foodcourt.traceability.domain.model.StatusChange;
import com.foodcourt.traceability.infrastructure.output.jpa.entity.OrderStatusChangeEntity;
import com.foodcourt.traceability.infrastructure.output.jpa.repository.OrderStatusChangeJpaRepository;

@ExtendWith(MockitoExtension.class)
class OrderStatusChangeRepositoryAdapterTest {

    private static final Instant OPENED = Instant.parse("2026-11-10T17:00:00Z");

    @Mock
    private OrderStatusChangeJpaRepository changes;

    @Mock
    private PlatformTransactionManager manager;

    @Mock
    private TransactionStatus transaction;

    private OrderStatusChangeRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        when(manager.getTransaction(any())).thenReturn(transaction);
        adapter = new OrderStatusChangeRepositoryAdapter(changes, manager);
    }

    @Test
    void saveInsertsANewRow() {
        when(changes.saveAndFlush(any(OrderStatusChangeEntity.class))).thenAnswer(invocation -> {
            OrderStatusChangeEntity entity = invocation.getArgument(0);
            return new OrderStatusChangeEntity(
                    4L,
                    entity.getOrderId(),
                    entity.getCustomerId(),
                    entity.getRestaurantId(),
                    entity.getEmployeeId(),
                    entity.getPreviousStatus(),
                    entity.getNewStatus(),
                    entity.getOccurredAt());
        });

        SaveResult result = adapter.save(opening(null));

        assertThat(result.created()).isTrue();
        assertThat(result.change().id()).isEqualTo(4L);
        verify(manager).commit(transaction);
    }

    @Test
    void saveReturnsTheStoredRowWhenTheSameFactIsInsertedTwice() {
        when(changes.saveAndFlush(any(OrderStatusChangeEntity.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate"));
        when(changes.findByOrderIdAndNewStatus(1L, OrderStatus.PENDING)).thenReturn(Optional.of(stored()));

        SaveResult result = adapter.save(opening(null));

        assertThat(result.created()).isFalse();
        assertThat(result.change().id()).isEqualTo(4L);
        assertThat(result.change().occurredAt()).isEqualTo(OPENED);
    }

    @Test
    void saveRejectsADuplicateStatusWithADifferentCustomer() {
        when(changes.saveAndFlush(any(OrderStatusChangeEntity.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate"));
        when(changes.findByOrderIdAndNewStatus(1L, OrderStatus.PENDING)).thenReturn(Optional.of(stored()));

        assertThatThrownBy(() -> adapter.save(new StatusChange(
                null, 1L, 8L, 1L, null, null, OrderStatus.PENDING, OPENED.plusSeconds(5), null)))
                .isInstanceOf(TraceConflict.class);
    }

    @Test
    void findByOrderIdReturnsTheRowsOldestFirst() {
        when(changes.findByOrderIdOrderByOccurredAtAscIdAsc(1L)).thenReturn(List.of(stored()));

        assertThat(adapter.findByOrderId(1L)).containsExactly(opening(4L));
    }

    private static StatusChange opening(Long id) {
        return new StatusChange(id, 1L, 9L, 1L, null, null, OrderStatus.PENDING, OPENED, null);
    }

    private static OrderStatusChangeEntity stored() {
        return new OrderStatusChangeEntity(4L, 1L, 9L, 1L, null, null, OrderStatus.PENDING, OPENED);
    }
}
