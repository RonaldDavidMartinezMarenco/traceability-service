package com.foodcourt.traceability.application.usecase;

import com.foodcourt.traceability.domain.exception.AccessDenied;
import com.foodcourt.traceability.domain.exception.AccountNotFound;
import com.foodcourt.traceability.domain.exception.RestaurantNotFound;
import com.foodcourt.traceability.domain.model.Account;
import com.foodcourt.traceability.domain.model.Caller;
import com.foodcourt.traceability.domain.model.RestaurantOwnership;
import com.foodcourt.traceability.domain.model.Role;
import com.foodcourt.traceability.domain.spi.AccountDirectory;
import com.foodcourt.traceability.domain.spi.RestaurantDirectory;

/**
 * The caller is an owner, the account is still an owner, and the restaurant belongs to that account.
 */
final class RestaurantOwnerRule {

    private final AccountDirectory accounts;
    private final RestaurantDirectory restaurants;

    RestaurantOwnerRule(AccountDirectory accounts, RestaurantDirectory restaurants) {
        this.accounts = accounts;
        this.restaurants = restaurants;
    }

    void require(Caller caller, long restaurantId) {
        requireOwnerRole(caller);
        requireAccount(caller);
        requireOwns(caller, restaurantId);
    }

    private static void requireOwnerRole(Caller caller) {
        if (caller.role() != Role.OWNER) {
            throw new AccessDenied();
        }
    }

    private void requireAccount(Caller caller) {
        Account account = accounts.findById(caller.id()).orElseThrow(AccountNotFound::new);
        if (account.role() != Role.OWNER) {
            throw new AccessDenied();
        }
    }

    private void requireOwns(Caller caller, long restaurantId) {
        RestaurantOwnership restaurant = restaurants.findById(restaurantId).orElseThrow(RestaurantNotFound::new);
        if (restaurant.ownerId() != caller.id()) {
            throw new AccessDenied();
        }
    }
}
