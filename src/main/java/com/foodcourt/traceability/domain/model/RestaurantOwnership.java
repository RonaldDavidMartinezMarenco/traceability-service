package com.foodcourt.traceability.domain.model;

/** Restaurant id and the account id of its owner, as plaza-service reports them. */
public record RestaurantOwnership(long id, long ownerId) {
}
