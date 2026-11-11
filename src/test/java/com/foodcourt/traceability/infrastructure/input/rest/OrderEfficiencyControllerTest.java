package com.foodcourt.traceability.infrastructure.input.rest;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.foodcourt.traceability.application.handler.GetOrderEfficiencyHandler;
import com.foodcourt.traceability.domain.exception.AccessDenied;
import com.foodcourt.traceability.domain.exception.RestaurantDirectoryUnavailable;
import com.foodcourt.traceability.domain.exception.RestaurantNotFound;
import com.foodcourt.traceability.domain.model.EmployeeRank;
import com.foodcourt.traceability.domain.model.OrderDuration;
import com.foodcourt.traceability.domain.model.OrderEfficiency;
import com.foodcourt.traceability.domain.model.PageQuery;
import com.foodcourt.traceability.domain.model.PageResult;
import com.foodcourt.traceability.infrastructure.exception.ApiExceptionHandler;
import com.foodcourt.traceability.infrastructure.security.SecurityConfig;
import com.foodcourt.traceability.infrastructure.security.SecurityProblemHandlers;

@WebMvcTest(OrderEfficiencyController.class)
@Import({ApiExceptionHandler.class, SecurityConfig.class, SecurityProblemHandlers.class})
class OrderEfficiencyControllerTest {

    private static final Instant START = Instant.parse("2026-11-10T17:05:00Z");
    private static final Instant END = Instant.parse("2026-11-10T17:15:00Z");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GetOrderEfficiencyHandler handler;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void efficiencyReturnsTheFinishedOrderAndTheRanking() throws Exception {
        when(handler.get(1L, new PageQuery(0, 10))).thenReturn(report());

        mockMvc.perform(get("/restaurants/1/efficiency").with(owner()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.restaurantId").value(1))
                .andExpect(jsonPath("$.completedOrders").value(1))
                .andExpect(jsonPath("$.averageDurationSeconds").value(600))
                .andExpect(jsonPath("$.orders.items[0].orderId").value(1))
                .andExpect(jsonPath("$.orders.items[0].employeeId").value(5))
                .andExpect(jsonPath("$.orders.items[0].durationSeconds").value(600))
                .andExpect(jsonPath("$.orders.items[0].slowerThanAverage").value(false))
                .andExpect(jsonPath("$.orders.totalPages").value(1))
                .andExpect(jsonPath("$.employeeRanking[0].rank").value(1))
                .andExpect(jsonPath("$.employeeRanking[0].employeeId").value(5));
        verify(handler).get(1L, new PageQuery(0, 10));
    }

    @Test
    void efficiencyReturnsAnEmptyReport() throws Exception {
        when(handler.get(1L, new PageQuery(0, 10)))
                .thenReturn(OrderEfficiency.of(1L, new PageResult<>(List.of(), 0, 10, 0), List.of()));

        mockMvc.perform(get("/restaurants/1/efficiency").with(owner()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completedOrders").value(0))
                .andExpect(jsonPath("$.averageDurationSeconds").isEmpty())
                .andExpect(jsonPath("$.orders.items").isEmpty())
                .andExpect(jsonPath("$.employeeRanking").isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"/restaurants/0/efficiency", "/restaurants/1/efficiency?page=-1",
            "/restaurants/1/efficiency?size=0", "/restaurants/1/efficiency?size=51"})
    void efficiencyRejectsAnInvalidRequest(String path) throws Exception {
        mockMvc.perform(get(path).with(owner())).andExpect(status().isBadRequest());
        verifyNoInteractions(handler);
    }

    @Test
    void efficiencyRejectsAMissingToken() throws Exception {
        mockMvc.perform(get("/restaurants/1/efficiency")).andExpect(status().isUnauthorized());
        verifyNoInteractions(handler);
    }

    @ParameterizedTest
    @ValueSource(strings = {"ROLE_CUSTOMER", "ROLE_EMPLOYEE", "ROLE_ADMINISTRATOR"})
    void efficiencyRejectsACallerWhoIsNotAnOwner(String role) throws Exception {
        mockMvc.perform(get("/restaurants/1/efficiency").with(jwt().authorities(new SimpleGrantedAuthority(role))))
                .andExpect(status().isForbidden());
        verifyNoInteractions(handler);
    }

    @Test
    void efficiencyRejectsAnOwnerOfAnotherRestaurant() throws Exception {
        when(handler.get(1L, new PageQuery(0, 10))).thenThrow(new AccessDenied());

        mockMvc.perform(get("/restaurants/1/efficiency").with(owner()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value("You do not have permission for this action"));
    }

    @Test
    void efficiencyHidesAMissingRestaurant() throws Exception {
        when(handler.get(9L, new PageQuery(0, 10))).thenThrow(new RestaurantNotFound());

        mockMvc.perform(get("/restaurants/9/efficiency").with(owner()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("No restaurant exists for this id"));
    }

    @Test
    void efficiencyReportsPlazaAsUnavailable() throws Exception {
        when(handler.get(1L, new PageQuery(0, 10))).thenThrow(new RestaurantDirectoryUnavailable());

        mockMvc.perform(get("/restaurants/1/efficiency").with(owner()))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.detail").value("The plaza service is unavailable"));
    }

    private static org.springframework.test.web.servlet.request.RequestPostProcessor owner() {
        return jwt().authorities(new SimpleGrantedAuthority("ROLE_OWNER"));
    }

    private static OrderEfficiency report() {
        OrderDuration order = new OrderDuration(1L, 5L, START, END, 600L, false);
        return new OrderEfficiency(
                1L,
                1,
                600L,
                new PageResult<>(List.of(order), 0, 10, 1),
                List.of(new EmployeeRank(1, 5L, 1, 600L)));
    }
}
