package com.foodcourt.traceability.infrastructure.output.client;

import java.io.IOException;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.foodcourt.traceability.domain.exception.RestaurantDirectoryUnavailable;
import com.foodcourt.traceability.domain.model.RestaurantOwnership;
import com.foodcourt.traceability.domain.spi.RestaurantDirectory;

/**
 * Reads {@code GET /restaurants/{id}} from plaza-service.
 * HTTP 404 is an empty restaurant. Any other non-2xx status, a timeout, or an
 * incomplete body is {@link RestaurantDirectoryUnavailable}.
 */
@Service
public final class PlazaRestaurantDirectory implements RestaurantDirectory {

    private final RestClient plazaService;

    public PlazaRestaurantDirectory(@Qualifier("plazaServiceClient") RestClient plazaService) {
        this.plazaService = plazaService;
    }

    @Override
    public Optional<RestaurantOwnership> findById(long id) {
        try {
            return plazaService.get().uri("/restaurants/{id}", id).exchange(this::read);
        } catch (RestClientException failure) {
            throw new RestaurantDirectoryUnavailable();
        }
    }

    private Optional<RestaurantOwnership> read(
            HttpRequest request,
            RestClient.RequestHeadersSpec.ConvertibleClientHttpResponse response) throws IOException {
        HttpStatusCode status = response.getStatusCode();
        if (status.value() == 404) {
            return Optional.empty();
        }
        if (!status.is2xxSuccessful()) {
            throw new RestaurantDirectoryUnavailable();
        }
        return Optional.of(ownership(response.bodyTo(RestaurantPayload.class)));
    }

    private static RestaurantOwnership ownership(RestaurantPayload body) {
        if (body == null || body.id() == null || body.ownerId() == null) {
            throw new RestaurantDirectoryUnavailable();
        }
        return new RestaurantOwnership(body.id(), body.ownerId());
    }

    /** Body of {@code GET /restaurants/{id}}. The domain does not depend on this type. */
    record RestaurantPayload(Long id, Long ownerId) {
    }
}
