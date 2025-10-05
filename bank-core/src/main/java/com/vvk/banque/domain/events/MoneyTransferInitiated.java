package com.vvk.banque.domain.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty; 
import com.vvk.banque.domain.ValueObj.AccountId;
import com.vvk.banque.domain.ValueObj.Money;

public record MoneyTransferInitiated(
    @JsonProperty("fromId") AccountId fromId, 
    @JsonProperty("toId") AccountId toId,    
    @JsonProperty("amount") Money amount     
) implements DomainEvent {
    @Override
    public AccountId accountId() {
        return fromId; // the transfer initiating account
    }
}
