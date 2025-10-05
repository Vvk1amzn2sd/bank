package com.vvk.banque.domain.events;

import com.vvk.banque.domain.ValueObj.AccountId;

public interface DomainEvent {
    default AccountId accountId() {
        if (this instanceof AccountOpened e)        return e.getAccountId();
        if (this instanceof MoneyDeposited e)       return e.accountId();
        if (this instanceof MoneyWithdrawn e)       return e.accountId();
        // The sender's ID is the primary ID for the MoneyTransferSend event
        if (this instanceof MoneyTransferSend e)    return e.fromAccountId();
        // The receiver's ID is the primary ID for the MoneyTransferReceive event
        if (this instanceof MoneyTransferReceive e) return e.toAccountId();
        throw new IllegalStateException("no accountId");
    }
    
    // Default implementation for source ID of a transfer
    default AccountId fromId() { 
        if (this instanceof MoneyTransferSend e)    return e.fromAccountId();
        if (this instanceof MoneyTransferReceive e) return e.fromAccountId();
        throw new UnsupportedOperationException(); 
    }
    
    // Default implementation for destination ID of a transfer
    default AccountId toId()   { 
        if (this instanceof MoneyTransferSend e)    return e.toAccountId();
        if (this instanceof MoneyTransferReceive e) return e.toAccountId();
        throw new UnsupportedOperationException(); 
    }
}
