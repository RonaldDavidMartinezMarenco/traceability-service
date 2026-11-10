package com.foodcourt.traceability.domain.model;

/** A status row that was inserted, or the same row already stored. */
public record SaveResult(StatusChange change, boolean created) {

    public static SaveResult created(StatusChange change) {
        return new SaveResult(change, true);
    }

    public static SaveResult existing(StatusChange change) {
        return new SaveResult(change, false);
    }
}
