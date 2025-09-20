package com.vvk.banque.domain.events;

import com.vvk.banque.domain.ValueObj.AccountId;
import com.vvk.banque.domain.ValueObj.CustomerId;
import com.vvk.banque.domain.ValueObj.Money;

public final class AccountOpened implements DomainEvent {
    private final AccountId accountId;
    private final CustomerId ownerId;
    private final Money openBal;

    public AccountOpened(AccountId accountId, CustomerId ownerId, Money openBal) {
        this.accountId = accountId;
        this.ownerId = ownerId;
        this.openBal = openBal;
    }

    public AccountId accountId() { return accountId; }
    public CustomerId ownerId() { return ownerId; }
    public Money openBal() { return openBal; }
}
