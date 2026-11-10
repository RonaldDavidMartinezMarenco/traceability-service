package com.foodcourt.traceability.infrastructure.input.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.foodcourt.traceability.application.handler.GetOrderTraceHandler;
import com.foodcourt.traceability.application.handler.RecordStatusChangeHandler;
import com.foodcourt.traceability.domain.exception.AccessDenied;
import com.foodcourt.traceability.domain.exception.AccountDirectoryUnavailable;
import com.foodcourt.traceability.domain.exception.IllegalStatusTransition;
import com.foodcourt.traceability.domain.exception.OrderTraceNotFound;
import com.foodcourt.traceability.domain.model.OrderStatus;
import com.foodcourt.traceability.domain.model.OrderTrace;
import com.foodcourt.traceability.domain.model.SaveResult;
import com.foodcourt.traceability.domain.model.StatusChange;
import com.foodcourt.traceability.domain.model.TraceDraft;
import com.foodcourt.traceability.infrastructure.exception.ApiExceptionHandler;
import com.foodcourt.traceability.infrastructure.security.SecurityConfig;
import com.foodcourt.traceability.infrastructure.security.SecurityProblemHandlers;

@WebMvcTest(OrderTraceController.class)
@Import({ApiExceptionHandler.class, SecurityConfig.class, SecurityProblemHandlers.class})
class OrderTraceControllerTest {

    private static final Instant OPENED = Instant.parse("2026-11-10T17:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RecordStatusChangeHandler recorder;

    @MockitoBean
    private GetOrderTraceHandler reader;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void recordReturnsCreatedWhenTheChangeIsNew() throws Exception {
        when(recorder.record(any(TraceDraft.class))).thenReturn(SaveResult.created(opening()));

        mockMvc.perform(post("/order-status-changes")
                        .with(customer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(openingBody()))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/orders/1/trace"))
                .andExpect(jsonPath("$.newStatus").value("PENDING"))
                .andExpect(jsonPath("$.previousStatus").isEmpty())
                .andExpect(jsonPath("$.employeeId").doesNotExist());
    }

    @Test
    void recordReturnsOkWhenTheChangeWasAlreadyStored() throws Exception {
        when(recorder.record(any(TraceDraft.class))).thenReturn(SaveResult.existing(opening()));

        mockMvc.perform(post("/order-status-changes")
                        .with(customer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(openingBody()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(1));
    }

    @Test
    void recordRejectsAMissingToken() throws Exception {
        mockMvc.perform(post("/order-status-changes").contentType(MediaType.APPLICATION_JSON).content(openingBody()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Authentication is required"));
        verifyNoInteractions(recorder);
    }

    @ParameterizedTest
    @ValueSource(strings = {"ROLE_ADMINISTRATOR", "ROLE_OWNER"})
    void recordRejectsARoleThatDoesNotMoveOrders(String authority) throws Exception {
        mockMvc.perform(post("/order-status-changes")
                        .with(jwt().authorities(new SimpleGrantedAuthority(authority)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(openingBody()))
                .andExpect(status().isForbidden());
        verifyNoInteractions(recorder);
    }

    @Test
    void recordRejectsAnUnknownStatus() throws Exception {
        mockMvc.perform(post("/order-status-changes")
                        .with(customer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"orderId":1,"customerId":9,"restaurantId":1,"newStatus":"COOKING"}
                                """))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(recorder);
    }

    @Test
    void recordRejectsAnIllegalTransition() throws Exception {
        when(recorder.record(any(TraceDraft.class))).thenThrow(new IllegalStatusTransition());

        mockMvc.perform(post("/order-status-changes")
                        .with(customer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(openingBody()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("The status change is not allowed"));
    }

    @Test
    void recordReportsTheDirectoryAsUnavailable() throws Exception {
        when(recorder.record(any(TraceDraft.class))).thenThrow(new AccountDirectoryUnavailable());

        mockMvc.perform(post("/order-status-changes")
                        .with(customer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(openingBody()))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void traceReturnsTheHistoryForACustomer() throws Exception {
        when(reader.get(1L)).thenReturn(OrderTrace.of(List.of(opening(), cancelled())));

        mockMvc.perform(get("/orders/1/trace").with(customer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(1))
                .andExpect(jsonPath("$.changes[0].newStatus").value("PENDING"))
                .andExpect(jsonPath("$.changes[0].secondsInStatus").value(120))
                .andExpect(jsonPath("$.changes[1].secondsInStatus").isEmpty())
                .andExpect(jsonPath("$.changes[0].employeeId").doesNotExist());
    }

    @Test
    void traceRejectsANonPositiveOrderId() throws Exception {
        mockMvc.perform(get("/orders/0/trace").with(customer())).andExpect(status().isBadRequest());
        verifyNoInteractions(reader);
    }

    @Test
    void traceRejectsAMissingToken() throws Exception {
        mockMvc.perform(get("/orders/1/trace"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(reader);
    }

    @Test
    void traceRejectsAnEmployee() throws Exception {
        mockMvc.perform(get("/orders/1/trace").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_EMPLOYEE"))))
                .andExpect(status().isForbidden());
        verifyNoInteractions(reader);
    }

    @Test
    void traceHidesAnOrderTheCustomerDoesNotOwn() throws Exception {
        when(reader.get(1L)).thenThrow(new OrderTraceNotFound());

        mockMvc.perform(get("/orders/1/trace").with(customer()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("No trace exists for this order"));
    }

    @Test
    void traceRejectsACustomerWhoseAccountCannotRead() throws Exception {
        when(reader.get(1L)).thenThrow(new AccessDenied());

        mockMvc.perform(get("/orders/1/trace").with(customer())).andExpect(status().isForbidden());
    }

    private static org.springframework.test.web.servlet.request.RequestPostProcessor customer() {
        return jwt().authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER"));
    }

    private static String openingBody() {
        return """
                {"orderId":1,"customerId":9,"restaurantId":1,"previousStatus":null,"newStatus":"PENDING"}
                """;
    }

    private static StatusChange opening() {
        return new StatusChange(1L, 1L, 9L, 1L, null, null, OrderStatus.PENDING, OPENED, 120L);
    }

    private static StatusChange cancelled() {
        return new StatusChange(
                2L, 1L, 9L, 1L, null, OrderStatus.PENDING, OrderStatus.CANCELLED, OPENED.plusSeconds(120), null);
    }
}
