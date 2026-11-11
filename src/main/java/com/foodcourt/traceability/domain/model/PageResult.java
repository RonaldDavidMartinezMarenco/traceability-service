package com.foodcourt.traceability.domain.model;

import java.util.List;

/**
 * One page of a list. {@code totalPages()} is 0 when the list is empty, so an empty
 * collection is not reported as one page.
 */
public record PageResult<T>(List<T> items, int page, int size, long totalItems) {

    public PageResult {
        items = List.copyOf(items);
    }

    public int totalPages() {
        return totalItems == 0 ? 0 : (int) ((totalItems + size - 1) / size);
    }
}
