package com.foodcourt.traceability.domain.spi;

import java.util.Optional;

import com.foodcourt.traceability.domain.model.Account;

/** Reads an account from user-service. */
public interface AccountDirectory {

    Optional<Account> findById(long id);
}
