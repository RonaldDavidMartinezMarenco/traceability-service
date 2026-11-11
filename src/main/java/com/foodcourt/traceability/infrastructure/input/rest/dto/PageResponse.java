package com.foodcourt.traceability.infrastructure.input.rest.dto;

import java.util.List;

/** One page of a list, with the totals the client needs to ask for the next page. */
public record PageResponse<T>(List<T> items, int page, int size, long totalItems, int totalPages) {
}
