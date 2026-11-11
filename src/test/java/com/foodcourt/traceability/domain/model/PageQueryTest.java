package com.foodcourt.traceability.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.foodcourt.traceability.domain.exception.InvalidPageRequest;

class PageQueryTest {

    @Test
    void acceptsTheDefaultPage() {
        PageQuery query = new PageQuery(0, 10);

        assertThat(query.page()).isZero();
        assertThat(query.size()).isEqualTo(10);
    }

    @Test
    void rejectsANegativePage() {
        assertThatThrownBy(() -> new PageQuery(-1, 10)).isInstanceOf(InvalidPageRequest.class);
    }

    @Test
    void rejectsASizeOutsideOneToFifty() {
        assertThatThrownBy(() -> new PageQuery(0, 0)).isInstanceOf(InvalidPageRequest.class);
        assertThatThrownBy(() -> new PageQuery(0, 51)).isInstanceOf(InvalidPageRequest.class);
    }

    @Test
    void rejectsAnOffsetThatDoesNotFitInAnInt() {
        assertThatThrownBy(() -> new PageQuery(50_000_000, 50)).isInstanceOf(InvalidPageRequest.class);
    }
}
