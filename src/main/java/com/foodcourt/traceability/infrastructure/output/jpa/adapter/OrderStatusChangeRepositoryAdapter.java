package com.foodcourt.traceability.infrastructure.output.jpa.adapter;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.foodcourt.traceability.domain.exception.TraceConflict;
import com.foodcourt.traceability.domain.model.CompletedInterval;
import com.foodcourt.traceability.domain.model.EmployeeTotal;
import com.foodcourt.traceability.domain.model.PageQuery;
import com.foodcourt.traceability.domain.model.PageResult;
import com.foodcourt.traceability.domain.model.SaveResult;
import com.foodcourt.traceability.domain.model.StatusChange;
import com.foodcourt.traceability.domain.spi.StatusChangeRepository;
import com.foodcourt.traceability.infrastructure.output.jpa.mapper.OrderStatusChangeJpaMapper;
import com.foodcourt.traceability.infrastructure.output.jpa.projection.CompletedIntervalRow;
import com.foodcourt.traceability.infrastructure.output.jpa.projection.EmployeeTotalRow;
import com.foodcourt.traceability.infrastructure.output.jpa.repository.OrderStatusChangeJpaRepository;

/**
 * JPA adapter for {@link StatusChangeRepository}.
 * A concurrent insert of the same status rolls back, then the stored row is read again.
 * The same fact is returned. A different fact is a conflict.
 */
@Service
public final class OrderStatusChangeRepositoryAdapter implements StatusChangeRepository {

    private final OrderStatusChangeJpaRepository changes;
    private final TransactionTemplate writes;
    private final TransactionTemplate reads;
    private final OrderStatusChangeJpaMapper mapper = new OrderStatusChangeJpaMapper();

    public OrderStatusChangeRepositoryAdapter(
            OrderStatusChangeJpaRepository changes,
            PlatformTransactionManager manager) {
        this.changes = changes;
        this.writes = new TransactionTemplate(manager);
        this.reads = new TransactionTemplate(manager);
        this.reads.setReadOnly(true);
    }

    @Override
    public List<StatusChange> findByOrderId(long orderId) {
        return reads.execute(status -> changes.findByOrderIdOrderByOccurredAtAscIdAsc(orderId).stream()
                .map(mapper::toDomain)
                .toList());
    }

    @Override
    public List<StatusChange> findByOrderAndCustomer(long orderId, long customerId) {
        return reads.execute(status -> changes
                .findByOrderIdAndCustomerIdOrderByOccurredAtAscIdAsc(orderId, customerId).stream()
                .map(mapper::toDomain)
                .toList());
    }

    @Override
    public PageResult<CompletedInterval> findCompleted(long restaurantId, PageQuery page) {
        Page<Object[]> rows = reads.execute(status -> changes.findCompleted(
                restaurantId, PageRequest.of(page.page(), page.size())));
        return intervals(page, rows);
    }

    @Override
    public List<EmployeeTotal> summarizeByEmployee(long restaurantId) {
        List<Object[]> rows = reads.execute(status -> changes.summarizeByEmployee(restaurantId));
        return totals(rows);
    }

    @Override
    public SaveResult save(StatusChange change) {
        try {
            return insert(change);
        } catch (DataIntegrityViolationException failure) {
            return reload(change);
        }
    }

    private SaveResult insert(StatusChange change) {
        StatusChange stored = writes.execute(status -> mapper.toDomain(changes.saveAndFlush(mapper.toEntity(change))));
        return SaveResult.created(stored);
    }

    private static PageResult<CompletedInterval> intervals(PageQuery page, Page<Object[]> rows) {
        List<CompletedInterval> items = new ArrayList<>();
        int dropped = 0;
        for (Object[] row : rows.getContent()) {
            dropped += keep(items, CompletedIntervalRow.from(row));
        }
        return new PageResult<>(items, page.page(), page.size(), rows.getTotalElements() - dropped);
    }

    /** A pair the domain rejects is left out of the page and out of the total. */
    private static int keep(List<CompletedInterval> items, CompletedIntervalRow row) {
        Optional<CompletedInterval> interval = row.accept();
        interval.ifPresent(items::add);
        return interval.isEmpty() ? 1 : 0;
    }

    private static List<EmployeeTotal> totals(List<Object[]> rows) {
        List<EmployeeTotal> totals = new ArrayList<>();
        for (Object[] row : rows) {
            totals.add(EmployeeTotalRow.from(row).toTotal());
        }
        return List.copyOf(totals);
    }

    private SaveResult reload(StatusChange change) {
        StatusChange stored = reads.execute(status -> changes
                .findByOrderIdAndNewStatus(change.orderId(), change.newStatus())
                .map(mapper::toDomain)
                .orElse(null));
        if (stored == null || !stored.sameFact(change)) {
            throw new TraceConflict();
        }
        return SaveResult.existing(stored);
    }
}
