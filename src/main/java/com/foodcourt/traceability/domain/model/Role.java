package com.foodcourt.traceability.domain.model;

/**
 * Account role names, matching {@code user-service}.
 * This service keeps its own copy; there is no shared jar.
 */
public enum Role {
    ADMINISTRATOR,
    OWNER,
    EMPLOYEE,
    CUSTOMER
}
