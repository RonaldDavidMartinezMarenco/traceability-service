package com.foodcourt.traceability.application.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.foodcourt.traceability.domain.api.GetOrderTrace;
import com.foodcourt.traceability.domain.api.RecordStatusChange;
import com.foodcourt.traceability.domain.model.Caller;
import com.foodcourt.traceability.domain.model.OrderStatus;
import com.foodcourt.traceability.domain.model.OrderTrace;
import com.foodcourt.traceability.domain.model.Role;
import com.foodcourt.traceability.domain.model.SaveResult;
import com.foodcourt.traceability.domain.model.StatusChange;
import com.foodcourt.traceability.domain.model.TraceDraft;
import com.foodcourt.traceability.domain.spi.CurrentCaller;

@ExtendWith(MockitoExtension.class)
class OrderTraceHandlerTest {

    private static final Caller RONALD = new Caller(9L, Role.CUSTOMER);
    private static final TraceDraft DRAFT = new TraceDraft(1L, 9L, 1L, null, null, OrderStatus.PENDING);

    @Mock
    private RecordStatusChange recordStatusChange;

    @Mock
    private GetOrderTrace getOrderTrace;

    @Mock
    private CurrentCaller currentCaller;

    @Test
    void recordForwardsTheCallerAndTheDraft() {
        when(currentCaller.require()).thenReturn(RONALD);
        SaveResult stored = SaveResult.created(new StatusChange(
                1L, 1L, 9L, 1L, null, null, OrderStatus.PENDING, Instant.parse("2026-11-10T17:00:00Z"), null));
        when(recordStatusChange.record(RONALD, DRAFT)).thenReturn(stored);
        RecordStatusChangeHandler handler = new RecordStatusChangeHandler(recordStatusChange, currentCaller);

        assertThat(handler.record(DRAFT)).isEqualTo(stored);
        verify(recordStatusChange).record(RONALD, DRAFT);
    }

    @Test
    void getForwardsTheCallerAndTheOrder() {
        when(currentCaller.require()).thenReturn(RONALD);
        OrderTrace trace = OrderTrace.of(List.of());
        when(getOrderTrace.get(RONALD, 1L)).thenReturn(trace);
        GetOrderTraceHandler handler = new GetOrderTraceHandler(getOrderTrace, currentCaller);

        assertThat(handler.get(1L)).isSameAs(trace);
    }
}
