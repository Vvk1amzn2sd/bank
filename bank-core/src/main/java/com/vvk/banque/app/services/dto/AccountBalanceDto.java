package com.vvk.banque.app.services.dto;

import com.vvk.banque.domain.ValueObj.Money;
import com.vvk.banque.domain.ValueObj.AccountId;

public final class AccountBalanceDto {

    private final AccountId accountId;
    private final Money balance;

    public AccountBalanceDto(AccountId accountId, Money balance) {
        this.accountId = accountId;
        this.balance = balance;
    }

    public AccountId getAccountId() {
        return accountId;
    }

    public Money getBalance() {
        return balance;
    }
 }
