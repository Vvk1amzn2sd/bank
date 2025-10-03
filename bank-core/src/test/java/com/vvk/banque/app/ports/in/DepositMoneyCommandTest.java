package com.vvk.banque.app.ports.in;
import com.vvk.banque.app.ports.FixedSeq;

import com.vvk.banque.domain.ValueObj.AccountId;
import com.vvk.banque.domain.ValueObj.Money;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.Currency;
import static org.junit.jupiter.api.Assertions.*;

class DepositMoneyCommandTest {

@Test
void executeDepositMoney_shouldNotThrow() {
    AccountId id = AccountId.generateUnique(new FixedSeq());
    Money amount = Money.of(BigDecimal.valueOf(100), Currency.getInstance("USD"));

    assertDoesNotThrow(() -> new StubCommand().executeDepositMoney(id, amount));
}

private static class StubCommand implements DepositMoneyCommand {
    @Override
    public void executeDepositMoney(AccountId accountId, Money amount) {
        // no-op stub
    }
}
}
