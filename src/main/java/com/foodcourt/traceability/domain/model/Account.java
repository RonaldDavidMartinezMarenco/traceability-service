package com.foodcourt.traceability.domain.model;

/**
 * Account summary read from user-service.
 * {@code restaurantId} is set only for an employee. Every other role carries null.
 */
public record Account(Long id, Role role, Long restaurantId) {
}
