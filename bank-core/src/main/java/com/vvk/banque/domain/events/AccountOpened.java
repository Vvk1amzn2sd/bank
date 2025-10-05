package com.vvk.banque.domain.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.vvk.banque.domain.ValueObj.AccountId;
import com.vvk.banque.domain.ValueObj.CustomerId;
import com.vvk.banque.domain.ValueObj.Money;

public final class AccountOpened implements DomainEvent {
    private final AccountId accountId;
    private final CustomerId ownerId;
    private final Money openBal;
    
    @JsonCreator
    public AccountOpened(
		    @JsonProperty("accountId") AccountId accountId, 
		    @JsonProperty("ownerId") CustomerId ownerId, 
		    @JsonProperty("openBal") Money openBal) {
        this.accountId = accountId;
        this.ownerId = ownerId;
        this.openBal = openBal;
    }

	//gttrs

    public AccountId getAccountId() { return accountId; }
    public CustomerId getOwnerId() { return ownerId; }
    public Money getOpenBal() { return openBal; }
}
