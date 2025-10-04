package com.vvk.banque.adapter.persistence			;

import com.vvk.banque.app.ports.out.AccountEventStorePort	;
import com.vvk.banque.app.ports.out.AccountQueryPort		;
import com.vvk.banque.domain.AggregatesObj.Account		;
import com.vvk.banque.domain.ValueObj.AccountId			;
import com.vvk.banque.domain.events.DomainEvent			;
import com.vvk.banque.domain.ValueObj.Money			;
import java.util.ArrayList					;
import java.util.Collection					;
import java.util.List						;
import java.util.Map						;
import java.util.concurrent.ConcurrentHashMap			;
import java.util.Collections					;
public class InMemoryAccountStore implements AccountEventStorePort, AccountQueryPort	{

	private final Map<AccountId, List<DomainEvent>> eventStore = new ConcurrentHashMap<>()	;

	@Override
	public void saveEvent(DomainEvent event)	{
		AccountId accountId = extractAccountId(event)	;
		if(accountId != null)	{
			eventStore.computeIfAbsent(accountId, k -> new ArrayList<>()).add(event);
		}
	}

@Override
    public List<DomainEvent> loadEvents(AccountId accountId) {
        return new ArrayList<>(eventStore.getOrDefault(accountId, Collections.emptyList()));
    }

    @Override
    public Account loadAccount(AccountId accountId) {
        List<DomainEvent> history = loadEvents(accountId);
        if (history.isEmpty()) {
            throw new RuntimeException("Account not found: " + accountId);
        }
        return Account.fromHistry(accountId, history);
    }

    @Override
    public Money findBalanceByAccountId(AccountId accountId) {
        Account account = loadAccount(accountId);
        return account.getBalance();
    }

    // helper: extract AccountId from any domain event
    private AccountId extractAccountId(DomainEvent event) {
        if (event instanceof com.vvk.banque.domain.events.AccountOpened e) {
            return e.getAccountId();
        } else if (event instanceof com.vvk.banque.domain.events.MoneyDeposited e) {
            return e.accountId();
        } else if (event instanceof com.vvk.banque.domain.events.MoneyWithdrawn e) {
            return e.accountId();
        } else if (event instanceof com.vvk.banque.domain.events.MoneyTransferSend e) {
            return e.fromAccountId();
        } else if (event instanceof com.vvk.banque.domain.events.MoneyTransferReceive e) {
            return e.toAccountId();
        }
        return null;
    }
}



