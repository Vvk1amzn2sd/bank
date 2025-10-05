package com.vvk.banque.domain.events;

import com.vvk.banque.domain.ValueObj.AccountId;

public interface DomainEvent {
    /* universal account-id extractor */
    default AccountId accountId() {
        if (this instanceof AccountOpened e)        return e.getAccountId();
        if (this instanceof MoneyDeposited e)       return e.accountId();
        if (this instanceof MoneyWithdrawn e)       return e.accountId();
        if (this instanceof MoneyTransferSend e)    return e.fromId();
        if (this instanceof MoneyTransferReceive e) return e.toId();
        throw new IllegalStateException("event has no accountId");
    }
}
