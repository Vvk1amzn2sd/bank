package com.vvk.banque.domain.events;

import com.fasterxml.jackson.annotation.JsonCreator; // 👈
import com.fasterxml.jackson.annotation.JsonProperty; // 
import com.vvk.banque.domain.ValueObj.AccountId;
import com.vvk.banque.domain.ValueObj.Money;

public final class MoneyTransferReceive implements DomainEvent {
    private final AccountId toAccountId;
    private final AccountId fromAccountId;
    private final Money amount;

    @JsonCreator
    public MoneyTransferReceive(
            @JsonProperty("toAccountId") AccountId toAccountId,
            @JsonProperty("fromAccountId") AccountId fromAccountId,
            @JsonProperty("amount") Money amount) {
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
