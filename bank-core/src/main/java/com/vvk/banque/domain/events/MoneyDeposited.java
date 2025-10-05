package com.vvk.banque.domain.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.vvk.banque.domain.ValueObj.AccountId;
import com.vvk.banque.domain.ValueObj.Money;

public final class MoneyDeposited implements DomainEvent {
    private final AccountId accountId;
    private final Money amount;

    @JsonCreator 
    public MoneyDeposited(
        @JsonProperty("accountId") AccountId accountId, // Change "accountID" to "accountId"
        @JsonProperty("amount") Money amount) {
        this.accountId = accountId;
        this.amount = amount;
    }

    public AccountId getAccountId() { return accountId; }
    public Money getAmount() { return amount; }

    public AccountId accountId() { return accountId; }
    public Money amount() { return amount; }
}
