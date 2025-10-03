package com.vvk.banque.app.ports.in;
import com.vvk.banque.app.ports.FixedSeq;

import com.vvk.banque.domain.ValueObj.AccountId;
import com.vvk.banque.domain.ValueObj.Money;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.Currency;
import static org.junit.jupiter.api.Assertions.*;

class WithdrawMoneyCommandTest {

@Test
void executeWithdrawMoney_shouldNotThrow() {
    AccountId id = AccountId.generateUnique(new FixedSeq());
    Money amount = Money.of(BigDecimal.valueOf(50), Currency.getInstance("USD"));

    assertDoesNotThrow(() -> new StubCommand().executeWithdrawMoney(id, amount));
}

private static class StubCommand implements WithdrawMoneyCommand {
    @Override
    public void executeWithdrawMoney(AccountId accountId, Money amount) {
        // no-op stub
    }
}
}
