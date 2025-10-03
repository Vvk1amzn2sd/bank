package com.vvk.banque.app.ports.in;
import com.vvk.banque.app.ports.FixedSeq;

import com.vvk.banque.domain.ValueObj.AccountId;
import com.vvk.banque.domain.ValueObj.Money;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.Currency;
import static org.junit.jupiter.api.Assertions.*;

class TransferMoneyCommandTest {

@Test
void executeTransferMoney_shouldNotThrow() {
    AccountId from = AccountId.generateUnique(new FixedSeq());
    AccountId to   = AccountId.generateUnique(new FixedSeq());
    Money amount   = Money.of(BigDecimal.valueOf(75), Currency.getInstance("USD"));

    assertDoesNotThrow(() -> new StubCommand().executeTransferMoney(from, to, amount));
}

private static class StubCommand implements TransferMoneyCommand {
    @Override
    public void executeTransferMoney(AccountId fromAccountId, AccountId toAccountId, Money amount) {
        // no-op stub
    }
}
}
