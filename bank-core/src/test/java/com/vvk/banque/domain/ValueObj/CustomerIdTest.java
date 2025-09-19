package com.vvk.banque.domain.ValueObj;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.*;

class CustomerIdTest {

    @Test
    void acceptsValidId() {
        assertThat(new CustomerId("A12").getCust()).isEqualTo("A12");
    }

    @ParameterizedTest
    @ValueSource(strings = {"AB", "ABCD", ""})
    void rejectsWrongLength(String id) {
        assertThatThrownBy(() -> new CustomerId(id))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must be exactly");
    }

    @ParameterizedTest
    @ValueSource(strings = {"1AB", "0CD", "9XX"})
    void rejectsFirstCharNumeric(String id) {
        assertThatThrownBy(() -> new CustomerId(id))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("First character");
    }

    @ParameterizedTest
    @ValueSource(strings = {"@BC", "A!@"})
    void rejectsNonAlphanumeric(String id) {
        assertThatThrownBy(() -> new CustomerId(id))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
