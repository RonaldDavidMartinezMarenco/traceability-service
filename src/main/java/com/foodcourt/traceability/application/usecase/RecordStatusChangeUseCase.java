package com.foodcourt.traceability.application.usecase;

import java.time.Clock;

import org.springframework.stereotype.Service;

import com.foodcourt.traceability.domain.api.RecordStatusChange;
import com.foodcourt.traceability.domain.model.Caller;
import com.foodcourt.traceability.domain.model.OrderTrace;
import com.foodcourt.traceability.domain.model.SaveResult;
import com.foodcourt.traceability.domain.model.StatusChange;
import com.foodcourt.traceability.domain.model.TraceDraft;
import com.foodcourt.traceability.domain.spi.AccountDirectory;
import com.foodcourt.traceability.domain.spi.StatusChangeRepository;

/**
 * Records one status change. The clock of this service stamps the row.
 * An identical change already stored is returned and is not inserted again.
 */
@Service
public final class RecordStatusChangeUseCase implements RecordStatusChange {

    private final StatusChangeRepository changes;
    private final TraceActorRule actors;
    private final Clock clock;

    public RecordStatusChangeUseCase(StatusChangeRepository changes, AccountDirectory accounts, Clock clock) {
        this.changes = changes;
        this.actors = new TraceActorRule(accounts);
        this.clock = clock;
    }

    @Override
    public SaveResult record(Caller caller, TraceDraft draft) {
        actors.requireWriter(caller, draft);
        OrderTrace history = OrderTrace.of(changes.findByOrderId(draft.orderId()));
        StatusChange accepted = history.accept(draft, clock.instant());
        if (accepted.id() != null) {
            return SaveResult.existing(accepted);
        }
        return changes.save(accepted);
    }
}
