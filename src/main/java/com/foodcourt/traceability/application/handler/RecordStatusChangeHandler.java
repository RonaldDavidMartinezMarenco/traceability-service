package com.foodcourt.traceability.application.handler;

import org.springframework.stereotype.Service;

import com.foodcourt.traceability.domain.api.RecordStatusChange;
import com.foodcourt.traceability.domain.model.SaveResult;
import com.foodcourt.traceability.domain.model.TraceDraft;
import com.foodcourt.traceability.domain.spi.CurrentCaller;

/** Entry the controller calls. The rules stay in {@link RecordStatusChange}. */
@Service
public final class RecordStatusChangeHandler {

    private final RecordStatusChange recordStatusChange;
    private final CurrentCaller currentCaller;

    public RecordStatusChangeHandler(RecordStatusChange recordStatusChange, CurrentCaller currentCaller) {
        this.recordStatusChange = recordStatusChange;
        this.currentCaller = currentCaller;
    }

    public SaveResult record(TraceDraft draft) {
        return recordStatusChange.record(currentCaller.require(), draft);
    }
}
