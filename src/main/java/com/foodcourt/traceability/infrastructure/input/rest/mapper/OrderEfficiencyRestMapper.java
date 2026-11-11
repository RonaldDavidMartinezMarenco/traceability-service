package com.foodcourt.traceability.infrastructure.input.rest.mapper;

import com.foodcourt.traceability.domain.model.EmployeeRank;
import com.foodcourt.traceability.domain.model.OrderDuration;
import com.foodcourt.traceability.domain.model.OrderEfficiency;
import com.foodcourt.traceability.domain.model.PageQuery;
import com.foodcourt.traceability.domain.model.PageResult;
import com.foodcourt.traceability.infrastructure.input.rest.dto.EmployeeRankResponse;
import com.foodcourt.traceability.infrastructure.input.rest.dto.OrderDurationResponse;
import com.foodcourt.traceability.infrastructure.input.rest.dto.OrderEfficiencyResponse;
import com.foodcourt.traceability.infrastructure.input.rest.dto.PageResponse;

/** Copies fields between the HTTP records and the domain. It applies no rule. */
public final class OrderEfficiencyRestMapper {

    public PageQuery toPageQuery(int page, int size) {
        return new PageQuery(page, size);
    }

    public OrderEfficiencyResponse toResponse(OrderEfficiency report) {
        return new OrderEfficiencyResponse(
                report.restaurantId(),
                report.completedOrders(),
                report.averageDurationSeconds(),
                toPage(report.orders()),
                report.employeeRanking().stream().map(this::toRank).toList());
    }

    private PageResponse<OrderDurationResponse> toPage(PageResult<OrderDuration> page) {
        return new PageResponse<>(
                page.items().stream().map(this::toDuration).toList(),
                page.page(),
                page.size(),
                page.totalItems(),
                page.totalPages());
    }

    private OrderDurationResponse toDuration(OrderDuration duration) {
        return new OrderDurationResponse(
                duration.orderId(),
                duration.employeeId(),
                duration.startedAt(),
                duration.finishedAt(),
                duration.durationSeconds(),
                duration.slowerThanAverage());
    }

    private EmployeeRankResponse toRank(EmployeeRank rank) {
        return new EmployeeRankResponse(
                rank.rank(),
                rank.employeeId(),
                rank.completedOrders(),
                rank.averageDurationSeconds());
    }
}
