package com.vvk.banque.domain.ValueObj;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class AccountIdTest {
	
	@Test
	void acceptsExactly5Digits() {
		AccountId id = new AccountId(00001);
		assertThat(id.getAcc()).isEqualTo(12345);
	}
/*
	@Test
	void rejectLT5() {
		assertThatIllegalArgumentException()
			.isThrownBy( () -> new AccountId(00000))
			.withMessageContaining("must be +ve");
	}

	@Test
	void rejectMT5() {
		assertThatIllegalArgumentException()
			.isThrownBy( () -> new AccountId(12388))
			.withMessageContaining("too long, exactly 5 allowed?");
	}	*/
}
