package com.vvk.banque.domain.AggregatesObj;

import com.vvk.banque.domain.ValueObj.*;
import com.vvk.banque.domain.events.*;
import com.vvk.banque.domain.exceptions.*;
import com.vvk.banque.domain.exceptions.AccountNullException;
import com.vvk.banque.domain.exceptions.InvalidTransferException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Currency;
import java.util.List;

public final class Account {

	private final	AccountId	aID;		
	private final	CustomerId	ownerId;
	private		Money		balance;	
	private		int		version;
	
	private final List<DomainEvent> uncommitted = new ArrayList<>();

	public static Account open(AccountId accId, CustomerId ownId, Money openBal) {
		if (openBal == null || openBal.isNegative()) {
			throw new OpeningBalanceNullException("Opening balance must be non-negative i.e. 0 or more");
		}
		if (openBal.isLT(Money.of(new BigDecimal("100"), Currency.getInstance("USD")))) {
			throw new OpeningBalanceNullException("Opening balance cannot be less than 100.00 cuz yk vvk? he is generous but can make an exception, after all he overrides system limitations from the terminal :)just reach out");
		}
		Account acc = new Account(accId, ownId, null);
		acc.record(new AccountOpened(accId, ownId, openBal));
		return acc;
	}

	public static Account fromHistry(AccountId id, List<DomainEvent> history) {
		AccountOpened openedEvent = history.stream()
				.filter(AccountOpened.class::isInstance)
				.map(AccountOpened.class::cast)
				.findFirst()
				.orElseThrow(() -> new AccountNullException("acc with id: " + id + "not found"));
		Account account = new Account(openedEvent.getAccountId(), openedEvent.getOwnerId(), openedEvent.getOpenBal());
		history.forEach(account::apply);
		return account;
	}

	private Account(AccountId aID, CustomerId ownerId, Money balance) {
		this.aID = aID;
		this.ownerId = ownerId;
		this.balance = balance;
		this.version = 0;
	}

	public AccountId getAID() { return aID; }
	public CustomerId getOwnerId() { return ownerId; }
	public Money getBalance() { return balance; }
	public List<DomainEvent> getUncommittedEvents() {
		return Collections.unmodifiableList(uncommitted);
	}
	public int getVersion() { return version; }
	public void markEventsAsCommitted() {
		uncommitted.clear();
	}

	public void deposit(Money amt) {
		if(!amt.isPositive()) throw new PositiveMoneyException("amt must be more than 0");
		record(new MoneyDeposited(aID, amt));
	}

	public void withdraw(Money amt) {
		if(!amt.isPositive()) throw new PositiveMoneyException("amt must be more than 0");
		if (balance.isLT(amt)) throw new InsufficientBalanceException("insufficient funs. u only have: " + balance);
		record(new MoneyWithdrawn(aID, amt));
	}

	public void send(AccountId to, Money amt) {
		if (to.equals(aID)) throw new InvalidTransferException("can't do transfer to same acc. it's not an infinite money glitch");
		if (balance.isLT(amt)) throw new InsufficientBalanceException("insufficient funs. can't transfer. u only have: " + balance);
		record(new MoneyTransferSend(aID, to, amt));
	}

	public void receive(AccountId from, Money amt) {
		if(!amt.isPositive()) throw new PositiveMoneyException("amt must be more than 0");
		record(new MoneyTransferReceive(aID, from, amt));
	}

    // NEW METHOD ADDED TO FIX COMPILATION ERRORS 5, 7
    // NOTE: This implementation is a temporary fix for compilation. 
    // The calling code (TransferMoneyCommandHandler) should likely be calling 
    // the 'send' method directly.
    public void transferTo(Account toAccount, Money amount) {
        // We will delegate to the 'send' method to enact the withdrawal/event
        this.send(toAccount.getAID(), amount);
    }
    
	private void record(DomainEvent event) {
		apply(event);
		uncommitted.add(event);
		this.version++;
	}

	private void apply(DomainEvent event) {
		if (event instanceof AccountOpened e) {
			balance = e.getOpenBal();
			version = 1;
		} else if (event instanceof MoneyDeposited e) {
			balance = balance.add(e.amount());
			version++;
		} else if (event instanceof MoneyWithdrawn e) {
			balance = balance.subtract(e.amount());
			version++;
		} else if (event instanceof MoneyTransferSend e) {
			balance = balance.subtract(e.amount());
			version++;
		} else if (event instanceof MoneyTransferReceive e) {
			balance = balance.add(e.amount());
			version++;
		}
	}
}
