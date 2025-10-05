package com.vvk.banque.domain.AggregatesObj;

import com.vvk.banque.domain.ValueObj.*;
import com.vvk.banque.domain.events.*;
import com.vvk.banque.domain.exceptions.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Currency;
import java.util.List;

public final class Account {

	private final	AccountId	aID;		
	private final	CustomerId	ownerId;		// only single acc now, might update in future to rflect joint acc util list
	private		Money		balance;	
	private		int		version;
	
/*----event-sourcing logging intended, hence account here is pure state m/c for emitting domain events---*/

	private final List<DomainEvent> uncommitted = new ArrayList<>();

/*----factory for new account---*/

	public static Account open(AccountId 	accId,
				   CustomerId 	ownId,
				   Money	openBal) {

		if (openBal == null || openBal.isNegative()) {
			throw new OpeningBalanceNullException("Opening balance must be non-negative i.e. 0 or more");
		}
		
		if (openBal.isGT(Money.of(new BigDecimal("1000"), Currency.getInstance("USD")))) {
			throw new OpeningBalanceNullException("Opening balance cannot be greater than 1000.00 or it can be, ask vvk? he overrides system limitations from the terminal :))");
		}

		Account  acc = new Account(accId,
						ownId,
						null);			
		acc.record(new AccountOpened(accId, ownId, openBal));
		return acc;
	}

/*-----re build from event stream---*/

	public static Account fromHistry(AccountId id
						,	List<DomainEvent>history) {

//find ownid and openbal from AccountOpened event
	
		AccountOpened openedEvent = history.stream()
				.filter(AccountOpened.class::isInstance)
				.map(AccountOpened.class::cast)
				.findFirst()
				.orElseThrow(() -> new AccountNotFoundException("acc with id: " + id + "not found"));

		Account account = new Account(openedEvent.accountId(),
						openedEvent.getOwnerId(),
						openedEvent.getOpenBal());
		
		history.forEach(account::apply);
		
		return account;
	}

/*---constructor- for event sourcing---*/

	private Account(AccountId aID, CustomerId ownerId, Money balance) {
		this.aID = aID;
		this.ownerId = ownerId;
		this.balance = balance;
		this.version = 0;
	}
	
/*---getters for the outside world---*/

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

/*---behaviourz---*/

	public void deposit(Money amt) {
		ensurePositive(amt);
		record(new MoneyDeposited(aID, amt));
	}

	public void withdraw(Money amt) {
		ensurePositive(amt);
		if (balance.isLT(amt)) {
			throw new InsufficientBalanceException("insufficient funs. u only have: " + balance);
		}
		record(new MoneyWithdrawn(aID, amt));
	}
	
	public void transferTo(Account benefic, Money amt) {

		ensurePositive(amt);

		if (benefic.getAID().equals(aID)) {
			throw new InvalidTransferException("can't do transfer to same acc. it's not an infinite money glitch");
		}
		if (balance.isLT(amt)) {
			throw new InsufficientBalanceException("insufficient funs. can't transfer. u only have: " + balance);
		}
		
// if transfer allowed to occur - emit 1 local, atomic event - initiation (debit)

		record(new MoneyTransferInitiated(aID, benefic.getAID(), amt));
	}

/*---helpers---*/

	private static void ensurePositive(Money m) {
		if(!m.isPositive()) {
			throw new PositiveMoneyException("amt must be more than 0");
		}
	}

	private void record(DomainEvent event) {
		apply(event);
		uncommitted.add(event);
		this.version++;
	}

/*-------state mutators- earlier we only recorded---*/
//not using switch cuz not supported below jdk 16 or 17?

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
			} else if (event instanceof MoneyTransferInitiated e) {
			 balance = balance.subtract(e.amount());
			 version++;
			} else if (event instanceof MoneyTransferReceive e) {
			 balance = balance.add(e.amount());
			 version++;
			} 
	}
}

