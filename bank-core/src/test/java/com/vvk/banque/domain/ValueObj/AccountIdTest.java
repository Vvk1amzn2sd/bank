package com.vvk.banque.domain.ValueObj;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class AccountIdTest {
	
	@Test
	void acceptsExactly5Digits() {
		AccountId id = new AccountId(10001);
		assertThat(id.getAcc()).isEqualTo(10001);
	}
}
