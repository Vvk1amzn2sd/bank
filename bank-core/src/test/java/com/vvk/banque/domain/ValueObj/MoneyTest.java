package com.vvk.banque.domain.ValueObj;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.Currency;

import static org.assertj.core.api.Assertions.*;

class MoneyTest {
	// not exactly needed as this is only domain, yet runnin' a single junit to test TDD
    private static final Currency EUR = Currency.getInstance("EUR");

    @Test
    void absReturnsPositive() {
        Money m = Money.of(new BigDecimal("-7.50"), EUR);
        Money expected = Money.of(new BigDecimal("7.50"), EUR);
        assertThat(m.abs()).isEqualTo(expected);
    }
}
