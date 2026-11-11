package com.foodcourt.traceability.domain.spi;

import java.util.Optional;

import com.foodcourt.traceability.domain.model.RestaurantOwnership;

/**
 * Reads restaurants from plaza-service. Empty when the restaurant does not exist.
 * Any other failure is {@link com.foodcourt.traceability.domain.exception.RestaurantDirectoryUnavailable}.
 */
public interface RestaurantDirectory {

    Optional<RestaurantOwnership> findById(long id);
}
