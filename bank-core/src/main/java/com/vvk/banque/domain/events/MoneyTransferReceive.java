package com.vvk.banque.domain.events;

import com.vvk.banque.domain.ValueObj.AccountId;
import com.vvk.banque.domain.ValueObj.Money;

public final class MoneyTransferReceive implements DomainEvent {
    private final AccountId toAccountId;
    private final AccountId fromAccountId;
    private final Money amount;

    public MoneyTransferReceive(AccountId toAccountId, AccountId fromAccountId, Money amount) {
        this.toAccountId = toAccountId;
        this.fromAccountId = fromAccountId;
        this.amount = amount;
    }

public AccountId getToAccountId() { return toAccountId; }
    public AccountId getFromAccountId() { return fromAccountId; }
    public Money getAmount() { return amount; }

    public AccountId toAccountId() { return toAccountId; }
    public AccountId fromAccountId() { return fromAccountId; }
    public Money amount() { return amount; }
}
