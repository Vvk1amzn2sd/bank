package com.vvk.banque.domain.ValueObj;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class AccountIdTest {
	
	@Test
	void acceptsExactly5Digits() {
		AccountId id = new AccountId(12345);
		assertThat(id.getAcc()).isEqualTo(12345);
	}

	@Test
	void rejectLT5() {
		assertThatIllegalArgumentException()
			.isThrownBy( () -> new AccountId(00))
			.withMessageContaining("only exactly 5 in length alloewd for acc id, k?");
	}

	@Test
	void rejectMT5() {
		assertThatIllegalArgumentException()
			.isThrownBy( () -> new AccountId(123889))
			.withMessageContaining("too long, exactly 5 allowed?");
	}
}
