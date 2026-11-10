package com.foodcourt.traceability.domain.api;

import com.foodcourt.traceability.domain.model.Caller;
import com.foodcourt.traceability.domain.model.SaveResult;
import com.foodcourt.traceability.domain.model.TraceDraft;

/** Records one status change of an order. */
public interface RecordStatusChange {

    SaveResult record(Caller caller, TraceDraft draft);
}
