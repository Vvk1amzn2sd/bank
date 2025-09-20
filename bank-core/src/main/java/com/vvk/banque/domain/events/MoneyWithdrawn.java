package com.vvk.banque.domain.events;

import com.vvk.banque.domain.ValueObj.AccountId;
import com.vvk.banque.domain.ValueObj.Money;

public final class MoneyWithdrawn implements DomainEvent {
    private final AccountId accountId;
    private final Money amount;

    public MoneyWithdrawn(AccountId accountId, Money amount) {
        this.accountId = accountId;
        this.amount = amount;
    }

    public AccountId accountId() { return accountId; }
    public Money amount() { return amount; }
}
