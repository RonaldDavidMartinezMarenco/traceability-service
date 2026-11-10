package com.foodcourt.traceability.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
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
import com.foodcourt.traceability.domain.exception.OrderTraceNotFound;
import com.foodcourt.traceability.domain.model.Account;
import com.foodcourt.traceability.domain.model.Caller;
import com.foodcourt.traceability.domain.model.OrderStatus;
import com.foodcourt.traceability.domain.model.OrderTrace;
import com.foodcourt.traceability.domain.model.Role;
import com.foodcourt.traceability.domain.model.StatusChange;
import com.foodcourt.traceability.domain.spi.AccountDirectory;
import com.foodcourt.traceability.domain.spi.StatusChangeRepository;

@ExtendWith(MockitoExtension.class)
class GetOrderTraceUseCaseTest {

    private static final Instant OPENED = Instant.parse("2026-11-10T17:00:00Z");
    private static final Caller RONALD = new Caller(9L, Role.CUSTOMER);

    @Mock
    private StatusChangeRepository changes;

    @Mock
    private AccountDirectory accounts;

    private GetOrderTraceUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetOrderTraceUseCase(changes, accounts);
    }

    @Test
    void getReturnsTheCustomersHistoryWithClosedDurations() {
        customer();
        when(changes.findByOrderAndCustomer(1L, RONALD.id())).thenReturn(List.of(pending(), cancelled()));

        OrderTrace trace = useCase.get(RONALD, 1L);

        assertThat(trace.changes()).extracting(StatusChange::newStatus)
                .containsExactly(OrderStatus.PENDING, OrderStatus.CANCELLED);
        assertThat(trace.changes()).extracting(StatusChange::secondsInStatus).containsExactly(120L, null);
    }

    @Test
    void getReturnsAHistoryThatOnlyReachedPending() {
        customer();
        when(changes.findByOrderAndCustomer(1L, RONALD.id())).thenReturn(List.of(pending()));

        OrderTrace trace = useCase.get(RONALD, 1L);

        assertThat(trace.changes()).hasSize(1);
        assertThat(trace.changes().get(0).secondsInStatus()).isNull();
    }

    @Test
    void getHidesAnOrderThatBelongsToSomeoneElse() {
        customer();
        when(changes.findByOrderAndCustomer(1L, RONALD.id())).thenReturn(List.of());

        assertThatThrownBy(() -> useCase.get(RONALD, 1L)).isInstanceOf(OrderTraceNotFound.class);
    }

    @ParameterizedTest
    @EnumSource(value = Role.class, names = {"ADMINISTRATOR", "OWNER", "EMPLOYEE"})
    void getRejectsACallerThatIsNotACustomer(Role role) {
        assertThatThrownBy(() -> useCase.get(new Caller(9L, role), 1L)).isInstanceOf(AccessDenied.class);
        verifyNoInteractions(accounts, changes);
    }

    @Test
    void getRejectsAnAccountThatIsNoLongerACustomer() {
        when(accounts.findById(RONALD.id())).thenReturn(Optional.of(new Account(RONALD.id(), Role.EMPLOYEE, 1L)));

        assertThatThrownBy(() -> useCase.get(RONALD, 1L)).isInstanceOf(AccessDenied.class);
        verifyNoInteractions(changes);
    }

    @Test
    void getRejectsAMissingAccount() {
        when(accounts.findById(RONALD.id())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.get(RONALD, 1L)).isInstanceOf(AccountNotFound.class);
        verifyNoInteractions(changes);
    }

    @Test
    void getReportsTheDirectoryAsUnavailable() {
        when(accounts.findById(RONALD.id())).thenThrow(new AccountDirectoryUnavailable());

        assertThatThrownBy(() -> useCase.get(RONALD, 1L)).isInstanceOf(AccountDirectoryUnavailable.class);
        verifyNoInteractions(changes);
    }

    private void customer() {
        when(accounts.findById(RONALD.id())).thenReturn(Optional.of(new Account(RONALD.id(), Role.CUSTOMER, null)));
    }

    private static StatusChange pending() {
        return new StatusChange(1L, 1L, 9L, 1L, null, null, OrderStatus.PENDING, OPENED, null);
    }

    private static StatusChange cancelled() {
        return new StatusChange(2L, 1L, 9L, 1L, null, OrderStatus.PENDING, OrderStatus.CANCELLED, OPENED.plusSeconds(120), null);
    }
}
