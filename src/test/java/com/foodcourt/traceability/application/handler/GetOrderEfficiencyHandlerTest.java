package com.foodcourt.traceability.application.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.foodcourt.traceability.domain.api.GetOrderEfficiency;
import com.foodcourt.traceability.domain.model.Caller;
import com.foodcourt.traceability.domain.model.OrderEfficiency;
import com.foodcourt.traceability.domain.model.PageQuery;
import com.foodcourt.traceability.domain.model.PageResult;
import com.foodcourt.traceability.domain.model.Role;
import com.foodcourt.traceability.domain.spi.CurrentCaller;

@ExtendWith(MockitoExtension.class)
class GetOrderEfficiencyHandlerTest {

    private static final Caller CARLOS = new Caller(2L, Role.OWNER);
    private static final PageQuery PAGE = new PageQuery(0, 10);

    @Mock
    private GetOrderEfficiency getOrderEfficiency;

    @Mock
    private CurrentCaller currentCaller;

    @Test
    void getForwardsTheCallerTheRestaurantAndThePage() {
        when(currentCaller.require()).thenReturn(CARLOS);
        OrderEfficiency report = OrderEfficiency.of(1L, new PageResult<>(List.of(), 0, 10, 0), List.of());
        when(getOrderEfficiency.get(CARLOS, 1L, PAGE)).thenReturn(report);
        GetOrderEfficiencyHandler handler = new GetOrderEfficiencyHandler(getOrderEfficiency, currentCaller);

        assertThat(handler.get(1L, PAGE)).isSameAs(report);
        verify(getOrderEfficiency).get(CARLOS, 1L, PAGE);
    }
}
