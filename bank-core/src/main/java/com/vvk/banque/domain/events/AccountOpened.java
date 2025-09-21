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

	//gttrs

    public AccountId getAccountId() { return accountId; }
    public CustomerId getOwnerId() { return ownerId; }
    public Money getOpenBal() { return openBal; }
}
