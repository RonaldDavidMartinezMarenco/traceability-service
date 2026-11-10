package com.foodcourt.traceability.application.usecase;

import com.foodcourt.traceability.domain.exception.AccessDenied;
import com.foodcourt.traceability.domain.exception.AccountNotFound;
import com.foodcourt.traceability.domain.exception.InvalidTraceRequest;
import com.foodcourt.traceability.domain.model.Account;
import com.foodcourt.traceability.domain.model.Caller;
import com.foodcourt.traceability.domain.model.OrderStatus;
import com.foodcourt.traceability.domain.model.Role;
import com.foodcourt.traceability.domain.model.TraceDraft;
import com.foodcourt.traceability.domain.spi.AccountDirectory;

/**
 * The caller's account still exists and matches the token.
 * A customer writes only their own opening and cancellation.
 * An employee writes only preparation, ready, and delivered, and only for their restaurant.
 */
final class TraceActorRule {

    private final AccountDirectory accounts;

    TraceActorRule(AccountDirectory accounts) {
        this.accounts = accounts;
    }

    void requireWriter(Caller caller, TraceDraft draft) {
        Account account = accounts.findById(caller.id()).orElseThrow(AccountNotFound::new);
        requireSameRole(caller, account);
        if (caller.role() == Role.CUSTOMER) {
            requireCustomer(caller, draft);
            return;
        }
        if (caller.role() == Role.EMPLOYEE) {
            requireEmployee(caller, account, draft);
            return;
        }
        throw new AccessDenied();
    }

    void requireReader(Caller caller) {
        if (caller.role() != Role.CUSTOMER) {
            throw new AccessDenied();
        }
        Account account = accounts.findById(caller.id()).orElseThrow(AccountNotFound::new);
        if (account.role() != Role.CUSTOMER) {
            throw new AccessDenied();
        }
    }

    private static void requireSameRole(Caller caller, Account account) {
        if (caller.role() != account.role()) {
            throw new AccessDenied();
        }
    }

    private static void requireCustomer(Caller caller, TraceDraft draft) {
        if (caller.id() != draft.customerId()) {
            throw new AccessDenied();
        }
        if (!customerStatus(draft.newStatus())) {
            throw new AccessDenied();
        }
    }

    private static boolean customerStatus(OrderStatus status) {
        return status == OrderStatus.PENDING || status == OrderStatus.CANCELLED;
    }

    private static void requireEmployee(Caller caller, Account account, TraceDraft draft) {
        if (!kitchen(draft.newStatus())) {
            throw new AccessDenied();
        }
        requireEmployeeIdentity(caller, draft);
        requireRestaurant(account, draft);
    }

    private static boolean kitchen(OrderStatus status) {
        return status == OrderStatus.IN_PREPARATION || status == OrderStatus.READY || status == OrderStatus.DELIVERED;
    }

    private static void requireEmployeeIdentity(Caller caller, TraceDraft draft) {
        if (draft.employeeId() == null) {
            throw new InvalidTraceRequest("An employee is required for this status");
        }
        if (caller.id() != draft.employeeId()) {
            throw new AccessDenied();
        }
    }

    private static void requireRestaurant(Account account, TraceDraft draft) {
        if (account.restaurantId() == null || account.restaurantId() != draft.restaurantId()) {
            throw new AccessDenied();
        }
    }
}
