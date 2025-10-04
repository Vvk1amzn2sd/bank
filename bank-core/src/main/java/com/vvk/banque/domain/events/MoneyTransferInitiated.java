package com.vvk.banque.domain.events;

import com.vvk.banque.domain.ValueObj.AccountId;
import com.vvk.banque.domain.ValueObj.Money;

public record MoneyTransferInitiated(AccountId fromId, AccountId toId, Money amount) implements DomainEvent {
    @Override
    public AccountId accountId() {
        return fromId; // the transfer initiating account
    }
}
