package com.foodcourt.traceability.infrastructure.output.jpa.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;

import com.foodcourt.traceability.domain.exception.TraceConflict;
import com.foodcourt.traceability.domain.model.CompletedInterval;
import com.foodcourt.traceability.domain.model.EmployeeTotal;
import com.foodcourt.traceability.domain.model.OrderStatus;
import com.foodcourt.traceability.domain.model.PageQuery;
import com.foodcourt.traceability.domain.model.PageResult;
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

    @Test
    void findCompletedKeepsAFinishedOrderAndDropsAnInvertedPair() {
        Object[] finished = new Object[] {1L, 1L, 5L, OPENED, OPENED.plusSeconds(600)};
        Object[] inverted = new Object[] {2L, 1L, 5L, OPENED.plusSeconds(600), OPENED};
        List<Object[]> rows = List.<Object[]>of(finished, inverted);
        when(changes.findCompleted(1L, PageRequest.of(0, 10)))
                .thenReturn(new PageImpl<>(rows, PageRequest.of(0, 10), 2));

        PageResult<CompletedInterval> page = adapter.findCompleted(1L, new PageQuery(0, 10));

        assertThat(page.items()).singleElement().satisfies(interval -> {
            assertThat(interval.orderId()).isEqualTo(1L);
            assertThat(interval.durationSeconds()).isEqualTo(600L);
        });
        assertThat(page.totalItems()).isEqualTo(1L);
    }

    @Test
    void findCompletedReadsOffsetAndSqlTimestamps() {
        OffsetDateTime started = OPENED.atOffset(ZoneOffset.UTC);
        OffsetDateTime finished = OPENED.plusSeconds(90).atOffset(ZoneOffset.UTC);
        Object[] offset = new Object[] {4L, 1L, 5L, started, finished};
        Object[] sql = new Object[] {
                5L,
                1L,
                6L,
                java.sql.Timestamp.from(OPENED),
                java.sql.Timestamp.from(OPENED.plusSeconds(30))
        };
        List<Object[]> rows = List.<Object[]>of(offset, sql);
        when(changes.findCompleted(1L, PageRequest.of(0, 10)))
                .thenReturn(new PageImpl<>(rows, PageRequest.of(0, 10), 2));

        PageResult<CompletedInterval> page = adapter.findCompleted(1L, new PageQuery(0, 10));

        assertThat(page.items()).extracting(CompletedInterval::durationSeconds).containsExactly(90L, 30L);
    }

    @Test
    void findCompletedRejectsAnUnexpectedTimestamp() {
        Object[] row = new Object[] {1L, 1L, 5L, "not-a-time", OPENED};
        List<Object[]> rows = List.<Object[]>of(row);
        when(changes.findCompleted(1L, PageRequest.of(0, 10)))
                .thenReturn(new PageImpl<>(rows, PageRequest.of(0, 10), 1));

        assertThatThrownBy(() -> adapter.findCompleted(1L, new PageQuery(0, 10)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void summarizeByEmployeeReturnsTheTotals() {
        when(changes.summarizeByEmployee(1L))
                .thenReturn(List.<Object[]>of(new Object[] {5L, 2, new java.math.BigDecimal("11")}));

        assertThat(adapter.summarizeByEmployee(1L)).containsExactly(new EmployeeTotal(5L, 2, 11L));
    }

    @Test
    void summarizeByEmployeeRejectsACountOfZero() {
        when(changes.summarizeByEmployee(1L)).thenReturn(List.<Object[]>of(new Object[] {5L, 0L, 0L}));

        assertThatThrownBy(() -> adapter.summarizeByEmployee(1L)).isInstanceOf(IllegalStateException.class);
    }

    private static StatusChange opening(Long id) {
        return new StatusChange(id, 1L, 9L, 1L, null, null, OrderStatus.PENDING, OPENED, null);
    }

    private static OrderStatusChangeEntity stored() {
        return new OrderStatusChangeEntity(4L, 1L, 9L, 1L, null, null, OrderStatus.PENDING, OPENED);
    }
}
