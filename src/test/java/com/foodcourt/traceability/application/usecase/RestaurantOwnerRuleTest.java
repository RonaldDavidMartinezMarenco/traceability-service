package com.foodcourt.traceability.application.usecase;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.foodcourt.traceability.domain.exception.AccessDenied;
import com.foodcourt.traceability.domain.exception.AccountNotFound;
import com.foodcourt.traceability.domain.exception.RestaurantNotFound;
import com.foodcourt.traceability.domain.model.Account;
import com.foodcourt.traceability.domain.model.Caller;
import com.foodcourt.traceability.domain.model.RestaurantOwnership;
import com.foodcourt.traceability.domain.model.Role;
import com.foodcourt.traceability.domain.spi.AccountDirectory;
import com.foodcourt.traceability.domain.spi.RestaurantDirectory;

@ExtendWith(MockitoExtension.class)
class RestaurantOwnerRuleTest {

    private static final Caller CARLOS = new Caller(2L, Role.OWNER);

    @Mock
    private AccountDirectory accounts;

    @Mock
    private RestaurantDirectory restaurants;

    private RestaurantOwnerRule rule;

    @BeforeEach
    void setUp() {
        rule = new RestaurantOwnerRule(accounts, restaurants);
    }

    @Test
    void requireAcceptsTheOwnerOfTheRestaurant() {
        ownerAccount();
        when(restaurants.findById(1L)).thenReturn(Optional.of(new RestaurantOwnership(1L, 2L)));

        rule.require(CARLOS, 1L);
    }

    @ParameterizedTest
    @EnumSource(value = Role.class, names = "OWNER", mode = EnumSource.Mode.EXCLUDE)
    void requireRejectsACallerWhoIsNotAnOwner(Role role) {
        assertThatThrownBy(() -> rule.require(new Caller(2L, role), 1L)).isInstanceOf(AccessDenied.class);
        verifyNoInteractions(accounts, restaurants);
    }

    @Test
    void requireRejectsAMissingAccount() {
        when(accounts.findById(2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> rule.require(CARLOS, 1L)).isInstanceOf(AccountNotFound.class);
        verifyNoInteractions(restaurants);
    }

    @Test
    void requireRejectsAnAccountThatIsNoLongerAnOwner() {
        when(accounts.findById(2L)).thenReturn(Optional.of(new Account(2L, Role.CUSTOMER, null)));

        assertThatThrownBy(() -> rule.require(CARLOS, 1L)).isInstanceOf(AccessDenied.class);
        verifyNoInteractions(restaurants);
    }

    @Test
    void requireRejectsAMissingRestaurant() {
        ownerAccount();
        when(restaurants.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> rule.require(CARLOS, 1L)).isInstanceOf(RestaurantNotFound.class);
    }

    @Test
    void requireRejectsAnotherOwnersRestaurant() {
        ownerAccount();
        when(restaurants.findById(1L)).thenReturn(Optional.of(new RestaurantOwnership(1L, 3L)));

        assertThatThrownBy(() -> rule.require(CARLOS, 1L)).isInstanceOf(AccessDenied.class);
    }

    private void ownerAccount() {
        when(accounts.findById(2L)).thenReturn(Optional.of(new Account(2L, Role.OWNER, null)));
    }
}
