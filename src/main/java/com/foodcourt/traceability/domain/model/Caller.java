package com.foodcourt.traceability.domain.model;

/**
 * Account that called the current request. The id matches {@code app_user.id}
 * in user-service, and the role is the one carried by the access token.
 */
public record Caller(long id, Role role) {
}
