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

	public static Account fromHistry(AccountId aID, List<DomainEvent> history) {
		if (history == null || history.isEmpty()) {
			throw new AccountNullException("Account with ID " + aID.toString() + " not found");
		}
		Account acc = new Account(aID, ((AccountOpened) history.get(0)).getOwnerId(), null);
		for (DomainEvent event : history) {
			acc.apply(event);
		}
		return acc;
	}

	/*--public commands--*/
	public void deposit(Money amt) {
		if(!amt.isPositive()) throw new PositiveMoneyException("amt must be more than 0");
		record(new MoneyDeposited(aID, amt));
	}

	public void withdraw(Money amt) {
		if(!amt.isPositive()) throw new PositiveMoneyException("amt must be more than 0");
		if (balance.isLT(amt)) throw new InsufficientBalanceException("insufficient funs. u only have: " + balance);
		record(new MoneyWithdrawn(aID, amt));
	}

	/*
	 * FIX 1: This method now records the new MoneyTransferInitiated event.
	 * It still only acts on the local aggregate (sender).
	 */
	public void send(AccountId to, Money amt) {
		if (to.equals(aID)) throw new InvalidTransferException("can't do transfer to same acc. it's not an infinite money glitch");
		if (balance.isLT(amt)) throw new InsufficientBalanceException("insufficient funs. can't transfer. u only have: " + balance);
		record(new MoneyTransferInitiated(aID, to, amt)); // <-- FIXED: Using MoneyTransferInitiated
	}

	/*
	 * FIX 2 & 3: The following two methods have been REMOVED for the single-aggregate principle:
	 * 1. public void transferTo(Account toAccount, Money amt) { ... } // Cross-aggregate interaction
	 * 2. public void receive(AccountId from, Money amt) { ... } // Receiving is now an event-driven process, not a sync command
	 */
	
	/*--private state change--*/
	private void record(DomainEvent event) {
		apply(event);
		uncommitted.add(event);
		this.version++; // This increment is typically managed by apply in other systems, but we keep the current pattern.
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
		// FIX 1: The correct event applied to the sender's stream (reduces balance)
		} else if (event instanceof MoneyTransferInitiated e) { 
			balance = balance.subtract(e.amount());
			version++;
		// Keep the old events for history replay consistency (though they should be deprecated)
		} else if (event instanceof MoneyTransferSend e) { 
			balance = balance.subtract(e.amount());
			version++;
		} else if (event instanceof MoneyTransferReceive e) {
			balance = balance.add(e.amount());
			version++;
		} else if (event instanceof CustomerSignedUp e) {
			// nothing to do here
		} else {
			throw new IllegalArgumentException("unhandled event type: " + event.getClass().getSimpleName());
		}
	}
	
	//gttrs

	public AccountId getID() { return aID; }
	public CustomerId getOwnerId() { return ownerId; }
	public Money getBalance() { return balance; }
	public int getVersion() { return version; }
	public List<DomainEvent> getUncommittedEvents() { return Collections.unmodifiableList(uncommitted); }
	public void markEventsAsCommitted() { uncommitted.clear(); }


	//constr
	private Account(AccountId aID, CustomerId ownerId, Money balance) {
		this.aID = aID;
		this.ownerId = ownerId;
		this.balance = balance;
		this.version = 0;
	}
}
