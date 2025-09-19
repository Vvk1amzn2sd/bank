package com.vvk.banque.domain.ValueObj;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.Currency;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MoneyTest {

	@Test
	void shouldThrowExceptionWhenAmountIsNegative() {
		BigDecimal negativeAmount = new BigDecimal("-100.50");
		Currency usd = Currency.getInstance("USD");
		
		assertThrows(IllegalArgumentException.class, () -> { new Money(negativeAmount, usd); } );
	}
	
}
