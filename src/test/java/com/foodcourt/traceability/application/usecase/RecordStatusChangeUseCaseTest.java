package com.foodcourt.traceability.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.foodcourt.traceability.domain.exception.AccessDenied;
import com.foodcourt.traceability.domain.exception.AccountDirectoryUnavailable;
import com.foodcourt.traceability.domain.exception.AccountNotFound;
import com.foodcourt.traceability.domain.exception.IllegalStatusTransition;
import com.foodcourt.traceability.domain.exception.InvalidTraceRequest;
import com.foodcourt.traceability.domain.model.Account;
import com.foodcourt.traceability.domain.model.Caller;
import com.foodcourt.traceability.domain.model.OrderStatus;
import com.foodcourt.traceability.domain.model.Role;
import com.foodcourt.traceability.domain.model.SaveResult;
import com.foodcourt.traceability.domain.model.StatusChange;
import com.foodcourt.traceability.domain.model.TraceDraft;
import com.foodcourt.traceability.domain.spi.AccountDirectory;
import com.foodcourt.traceability.domain.spi.StatusChangeRepository;

@ExtendWith(MockitoExtension.class)
class RecordStatusChangeUseCaseTest {

    private static final Instant NOW = Instant.parse("2026-11-10T17:00:00Z");
    private static final Caller RONALD = new Caller(9L, Role.CUSTOMER);
    private static final Caller PEDRO = new Caller(5L, Role.EMPLOYEE);

    @Mock
    private StatusChangeRepository changes;

    @Mock
    private AccountDirectory accounts;

    private RecordStatusChangeUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new RecordStatusChangeUseCase(changes, accounts, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void recordStoresACustomersOpening() {
        customer(RONALD);
        when(changes.findByOrderId(1L)).thenReturn(List.of());
        when(changes.save(any(StatusChange.class))).thenAnswer(invocation -> SaveResult.created(withId(invocation.getArgument(0), 10L)));

        SaveResult result = useCase.record(RONALD, opening());

        assertThat(result.created()).isTrue();
        assertThat(result.change().id()).isEqualTo(10L);
        assertThat(result.change().occurredAt()).isEqualTo(NOW);
        assertThat(result.change().employeeId()).isNull();
        assertThat(result.change().newStatus()).isEqualTo(OrderStatus.PENDING);
    }

    @Test
    void recordStoresACancellationAfterPending() {
        customer(RONALD);
        when(changes.findByOrderId(1L)).thenReturn(List.of(storedOpening()));
        when(changes.save(any(StatusChange.class))).thenAnswer(invocation -> SaveResult.created(withId(invocation.getArgument(0), 11L)));

        SaveResult result = useCase.record(RONALD, new TraceDraft(1L, 9L, 1L, null, OrderStatus.PENDING, OrderStatus.CANCELLED));

        assertThat(result.created()).isTrue();
        assertThat(result.change().newStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void recordStoresPreparationForTheEmployeeOfThatRestaurant() {
        employee(PEDRO, 1L);
        when(changes.findByOrderId(1L)).thenReturn(List.of(storedOpening()));
        when(changes.save(any(StatusChange.class))).thenAnswer(invocation -> SaveResult.created(withId(invocation.getArgument(0), 12L)));

        SaveResult result = useCase.record(PEDRO, new TraceDraft(1L, 9L, 1L, PEDRO.id(), OrderStatus.PENDING, OrderStatus.IN_PREPARATION));

        assertThat(result.created()).isTrue();
        assertThat(result.change().employeeId()).isEqualTo(PEDRO.id());
    }

    @Test
    void recordReturnsTheStoredRowWhenTheSameChangeIsRepeated() {
        customer(RONALD);
        when(changes.findByOrderId(1L)).thenReturn(List.of(storedOpening()));

        SaveResult result = useCase.record(RONALD, opening());

        assertThat(result.created()).isFalse();
        assertThat(result.change()).isEqualTo(storedOpening());
        verify(changes, never()).save(any());
    }

    @Test
    void recordRejectsACustomerWritingAKitchenStatus() {
        customer(RONALD);

        assertThatThrownBy(() -> useCase.record(RONALD, new TraceDraft(1L, 9L, 1L, RONALD.id(), OrderStatus.PENDING, OrderStatus.IN_PREPARATION)))
                .isInstanceOf(AccessDenied.class);
        verify(changes, never()).findByOrderId(any(Long.class));
        verify(changes, never()).save(any());
    }

    @Test
    void recordRejectsACustomerWritingAnotherCustomersOrder() {
        customer(RONALD);

        assertThatThrownBy(() -> useCase.record(RONALD, new TraceDraft(1L, 8L, 1L, null, null, OrderStatus.PENDING)))
                .isInstanceOf(AccessDenied.class);
        verify(changes, never()).save(any());
    }

    @Test
    void recordRejectsAnEmployeeWritingTheOpening() {
        employee(PEDRO, 1L);

        assertThatThrownBy(() -> useCase.record(PEDRO, new TraceDraft(1L, 9L, 1L, PEDRO.id(), null, OrderStatus.PENDING)))
                .isInstanceOf(AccessDenied.class);
        verify(changes, never()).save(any());
    }

    @Test
    void recordRejectsAKitchenStatusWithoutAnEmployee() {
        employee(PEDRO, 1L);

        assertThatThrownBy(() -> useCase.record(PEDRO, new TraceDraft(1L, 9L, 1L, null, OrderStatus.PENDING, OrderStatus.IN_PREPARATION)))
                .isInstanceOf(InvalidTraceRequest.class)
                .hasMessage("An employee is required for this status");
        verify(changes, never()).save(any());
    }

    @Test
    void recordRejectsAnEmployeeAttributingTheChangeToSomeoneElse() {
        employee(PEDRO, 1L);

        assertThatThrownBy(() -> useCase.record(PEDRO, new TraceDraft(1L, 9L, 1L, 6L, OrderStatus.PENDING, OrderStatus.IN_PREPARATION)))
                .isInstanceOf(AccessDenied.class);
        verify(changes, never()).save(any());
    }

    @Test
    void recordRejectsAnEmployeeOfAnotherRestaurant() {
        employee(PEDRO, 2L);

        assertThatThrownBy(() -> useCase.record(PEDRO, new TraceDraft(1L, 9L, 1L, PEDRO.id(), OrderStatus.PENDING, OrderStatus.IN_PREPARATION)))
                .isInstanceOf(AccessDenied.class);
        verify(changes, never()).save(any());
    }

    @Test
    void recordRejectsAnEmployeeWithoutARestaurant() {
        employee(PEDRO, null);

        assertThatThrownBy(() -> useCase.record(PEDRO, new TraceDraft(1L, 9L, 1L, PEDRO.id(), OrderStatus.PENDING, OrderStatus.IN_PREPARATION)))
                .isInstanceOf(AccessDenied.class);
        verify(changes, never()).save(any());
    }

    @ParameterizedTest
    @EnumSource(value = Role.class, names = {"ADMINISTRATOR", "OWNER"})
    void recordRejectsARoleThatDoesNotMoveOrders(Role role) {
        when(accounts.findById(1L)).thenReturn(Optional.of(new Account(1L, role, null)));

        assertThatThrownBy(() -> useCase.record(new Caller(1L, role), opening()))
                .isInstanceOf(AccessDenied.class);
        verify(changes, never()).save(any());
    }

    @Test
    void recordRejectsATokenWhoseRoleNoLongerMatchesTheAccount() {
        when(accounts.findById(RONALD.id())).thenReturn(Optional.of(new Account(RONALD.id(), Role.EMPLOYEE, 1L)));

        assertThatThrownBy(() -> useCase.record(RONALD, opening())).isInstanceOf(AccessDenied.class);
        verify(changes, never()).save(any());
    }

    @Test
    void recordRejectsAMissingAccount() {
        when(accounts.findById(RONALD.id())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.record(RONALD, opening())).isInstanceOf(AccountNotFound.class);
        verify(changes, never()).save(any());
    }

    @Test
    void recordReportsTheDirectoryAsUnavailable() {
        when(accounts.findById(RONALD.id())).thenThrow(new AccountDirectoryUnavailable());

        assertThatThrownBy(() -> useCase.record(RONALD, opening())).isInstanceOf(AccountDirectoryUnavailable.class);
        verify(changes, never()).save(any());
    }

    @Test
    void recordRejectsAnIllegalTransitionWithoutSaving() {
        customer(RONALD);
        when(changes.findByOrderId(1L)).thenReturn(List.of());

        assertThatThrownBy(() -> useCase.record(RONALD, new TraceDraft(1L, 9L, 1L, null, OrderStatus.PENDING, OrderStatus.CANCELLED)))
                .isInstanceOf(IllegalStatusTransition.class);
        verify(changes, never()).save(any());
    }

    private void customer(Caller caller) {
        when(accounts.findById(caller.id())).thenReturn(Optional.of(new Account(caller.id(), Role.CUSTOMER, null)));
    }

    private void employee(Caller caller, Long restaurantId) {
        when(accounts.findById(caller.id())).thenReturn(Optional.of(new Account(caller.id(), Role.EMPLOYEE, restaurantId)));
    }

    private static TraceDraft opening() {
        return new TraceDraft(1L, 9L, 1L, null, null, OrderStatus.PENDING);
    }

    private static StatusChange storedOpening() {
        return new StatusChange(10L, 1L, 9L, 1L, null, null, OrderStatus.PENDING, NOW.minusSeconds(30), null);
    }

    private static StatusChange withId(StatusChange change, long id) {
        return new StatusChange(
                id,
                change.orderId(),
                change.customerId(),
                change.restaurantId(),
                change.employeeId(),
                change.previousStatus(),
                change.newStatus(),
                change.occurredAt(),
                change.secondsInStatus());
    }
}
