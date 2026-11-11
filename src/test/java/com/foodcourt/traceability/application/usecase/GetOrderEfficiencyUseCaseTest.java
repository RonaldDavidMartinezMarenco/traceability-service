package com.foodcourt.traceability.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
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
import com.foodcourt.traceability.domain.exception.RestaurantDirectoryUnavailable;
import com.foodcourt.traceability.domain.model.Account;
import com.foodcourt.traceability.domain.model.Caller;
import com.foodcourt.traceability.domain.model.CompletedInterval;
import com.foodcourt.traceability.domain.model.EmployeeTotal;
import com.foodcourt.traceability.domain.model.OrderEfficiency;
import com.foodcourt.traceability.domain.model.PageQuery;
import com.foodcourt.traceability.domain.model.PageResult;
import com.foodcourt.traceability.domain.model.RestaurantOwnership;
import com.foodcourt.traceability.domain.model.Role;
import com.foodcourt.traceability.domain.spi.AccountDirectory;
import com.foodcourt.traceability.domain.spi.RestaurantDirectory;
import com.foodcourt.traceability.domain.spi.StatusChangeRepository;

@ExtendWith(MockitoExtension.class)
class GetOrderEfficiencyUseCaseTest {

    private static final Caller CARLOS = new Caller(2L, Role.OWNER);
    private static final PageQuery PAGE = new PageQuery(0, 10);
    private static final Instant START = Instant.parse("2026-11-10T17:05:00Z");

    @Mock
    private StatusChangeRepository changes;

    @Mock
    private AccountDirectory accounts;

    @Mock
    private RestaurantDirectory restaurants;

    private GetOrderEfficiencyUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetOrderEfficiencyUseCase(changes, accounts, restaurants);
    }

    @Test
    void getReturnsDurationsAndTheRankingForTheOwnedRestaurant() {
        owner();
        when(changes.findCompleted(1L, PAGE)).thenReturn(new PageResult<>(List.of(slow()), 0, 10, 2));
        when(changes.summarizeByEmployee(1L)).thenReturn(List.of(new EmployeeTotal(8L, 1, 5), new EmployeeTotal(7L, 1, 6)));

        OrderEfficiency report = useCase.get(CARLOS, 1L, PAGE);

        assertThat(report.restaurantId()).isEqualTo(1L);
        assertThat(report.averageDurationSeconds()).isEqualTo(5L);
        assertThat(report.orders().items().get(0).slowerThanAverage()).isTrue();
        assertThat(report.employeeRanking()).extracting(rank -> rank.employeeId()).containsExactly(8L, 7L);
        verify(changes).findCompleted(1L, PAGE);
        verify(changes).summarizeByEmployee(1L);
    }

    @Test
    void getReturnsAnEmptyReportWhenTheRestaurantHasNoFinishedOrder() {
        owner();
        when(changes.findCompleted(1L, PAGE)).thenReturn(new PageResult<>(List.of(), 0, 10, 0));
        when(changes.summarizeByEmployee(1L)).thenReturn(List.of());

        OrderEfficiency report = useCase.get(CARLOS, 1L, PAGE);

        assertThat(report.completedOrders()).isZero();
        assertThat(report.averageDurationSeconds()).isNull();
        assertThat(report.employeeRanking()).isEmpty();
    }

    @ParameterizedTest
    @EnumSource(value = Role.class, names = "OWNER", mode = EnumSource.Mode.EXCLUDE)
    void getRejectsACallerWhoIsNotAnOwner(Role role) {
        assertThatThrownBy(() -> useCase.get(new Caller(2L, role), 1L, PAGE)).isInstanceOf(AccessDenied.class);
        verifyNoInteractions(accounts, restaurants, changes);
    }

    @Test
    void getDoesNotReadOrdersWhenTheAccountDirectoryIsDown() {
        when(accounts.findById(2L)).thenThrow(new AccountDirectoryUnavailable());

        assertThatThrownBy(() -> useCase.get(CARLOS, 1L, PAGE)).isInstanceOf(AccountDirectoryUnavailable.class);
        verifyNoInteractions(restaurants, changes);
    }

    @Test
    void getDoesNotReadOrdersWhenPlazaIsDown() {
        when(accounts.findById(2L)).thenReturn(Optional.of(new Account(2L, Role.OWNER, null)));
        when(restaurants.findById(1L)).thenThrow(new RestaurantDirectoryUnavailable());

        assertThatThrownBy(() -> useCase.get(CARLOS, 1L, PAGE)).isInstanceOf(RestaurantDirectoryUnavailable.class);
        verifyNoInteractions(changes);
    }

    private void owner() {
        when(accounts.findById(2L)).thenReturn(Optional.of(new Account(2L, Role.OWNER, null)));
        when(restaurants.findById(1L)).thenReturn(Optional.of(new RestaurantOwnership(1L, 2L)));
    }

    private static CompletedInterval slow() {
        return new CompletedInterval(4L, 1L, 7L, START, START.plusSeconds(6));
    }
}
