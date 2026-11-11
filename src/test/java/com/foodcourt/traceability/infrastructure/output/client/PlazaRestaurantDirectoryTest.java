package com.foodcourt.traceability.infrastructure.output.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.net.SocketTimeoutException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.foodcourt.traceability.domain.exception.RestaurantDirectoryUnavailable;
import com.foodcourt.traceability.domain.model.RestaurantOwnership;

class PlazaRestaurantDirectoryTest {

    private static final String URL = "http://plaza-service/restaurants/1";

    private MockRestServiceServer server;
    private PlazaRestaurantDirectory directory;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://plaza-service");
        server = MockRestServiceServer.bindTo(builder).build();
        directory = new PlazaRestaurantDirectory(builder.build());
    }

    @Test
    void findByIdReturnsTheOwnerOfTheRestaurant() {
        server.expect(requestTo(URL))
                .andRespond(withSuccess("{\"id\":1,\"ownerId\":2}", MediaType.APPLICATION_JSON));

        assertThat(directory.findById(1L)).contains(new RestaurantOwnership(1L, 2L));
        server.verify();
    }

    @Test
    void findByIdIsEmptyWhenTheRestaurantIsMissing() {
        server.expect(requestTo(URL)).andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThat(directory.findById(1L)).isEmpty();
        server.verify();
    }

    @ParameterizedTest
    @EnumSource(value = HttpStatus.class, names = {"UNAUTHORIZED", "FORBIDDEN", "INTERNAL_SERVER_ERROR", "SERVICE_UNAVAILABLE"})
    void findByIdReportsAnUnexpectedStatusAsUnavailable(HttpStatus status) {
        server.expect(requestTo(URL)).andRespond(withStatus(status));

        assertThatThrownBy(() -> directory.findById(1L)).isInstanceOf(RestaurantDirectoryUnavailable.class);
        server.verify();
    }

    @Test
    void findByIdReportsATimeoutAsUnavailable() {
        server.expect(requestTo(URL)).andRespond(withException(new SocketTimeoutException("read timed out")));

        assertThatThrownBy(() -> directory.findById(1L)).isInstanceOf(RestaurantDirectoryUnavailable.class);
        server.verify();
    }

    @Test
    void findByIdReportsAnIncompleteBodyAsUnavailable() {
        server.expect(requestTo(URL)).andRespond(withSuccess("{\"id\":1}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> directory.findById(1L)).isInstanceOf(RestaurantDirectoryUnavailable.class);
        server.verify();
    }
}
