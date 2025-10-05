package com.vvk.banque.domain.events;

import com.vvk.banque.domain.ValueObj.AccountId;
import com.vvk.banque.domain.ValueObj.Money;

public final class MoneyTransferSend implements DomainEvent {
    private final AccountId fromAccountId;
    private final AccountId toAccountId;
    private final Money amount;

    public MoneyTransferSend(AccountId fromAccountId, AccountId toAccountId, Money amount) {
        this.fromAccountId = fromAccountId;
        this.toAccountId = toAccountId;
        this.amount = amount;
    }

public AccountId getFromAccountId() { return fromAccountId; }
    public AccountId getToAccountId() { return toAccountId; }
    public Money getAmount() { return amount; }

    public AccountId fromAccountId() { return fromAccountId; }
    public AccountId toAccountId() { return toAccountId; }
    public Money amount() { return amount; }
}
