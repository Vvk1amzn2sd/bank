package com.vvk.banque.domain.AggregatesObj;

import com.vvk.banque.domain.ValueObj.*;
import com.vvk.banque.domain.events.*;
import com.vvk.banque.domain.exceptions.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class Account {

	private final	AccountId	aID;		
	private final	CustomerId	ownerId;		// only single acc now, might update in future to rflect joint acc util list
	private		Money		balance;	
	
/*----event-sourcing logging intended, hence account here is pure state m/c for emitting domain events---*/

	private final List<DomainEvent> uncommitted = new ArrayList<>();

/*----factory for new account---*/

	public static Account open(AccountId 	accId,
				   CustomerId 	ownId,
				   Money	openBal) {

		if (openBal == null || openBal.isNegative()) {
			throw new OpeningBalanceNullException("Opening balance must be non-negative i.e. 0 or more");
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
				.orElseThrow(() -> new AccountOpenedException("History must contain acc opend event"));


//create acc w/ owner id and balance from openedEvent

			Account acc = new Account(id, openedEvent.ownerId(), openedEvent.openBal()); 
				history.forEach(acc::apply); 	/*--apply all events to rebuild state including acc opend, redundant but safe at this stage, will add snapshot later for efficiency if time permits--*/
					return acc;
			}

	// pvt cnstrctr
	private Account(AccountId aID, CustomerId ownerId, Money balance) {
		this.aID	= aID;
		this.ownerId	= ownerId;
		this.balance	= balance;
	}

	//getters

	public AccountId getAID() { return aID; }
	public CustomerId getOwnerId() { return ownerId; }
	public Money getBalance() { return balance; }
	
	public List<DomainEvent> getUncommittedEvents() {
		return Collections.unmodifiableList(uncommitted); 
		}
	
	public void markEventsAsCommitted() { uncommitted.clear(); }
	
	
/*-----defining vehavior below - this is not a usecase still, this is raw behvior, much like domain is suppose to be--*/

	/*---deposits---*/
	
	public void deposit(Money amt) {
		ensurePositive(amt);
		record(new MoneyDeposited(aID, amt)); // emit add events
	}

	/*---withdrawals----*/

	public void withdraw(Money amt) {
		ensurePositive(amt);
		if (balance.isLT(amt)) {
			throw new InsufficientBalanceException("insufficient funds. oops! you currently only have" + balance +"in ur acc");
			}
	record(new MoneyWithdrawn(aID, amt));	}

	/*---transfers----*/

	public void transferTo(Account benefic, Money amt) {
		
	//cant transfer to same acc
		
		    if (benefic == null) {
        throw new BeneficiaryAccountNullException("Beneficiary account cannot be null");
    }
		ensurePositive(amt);
		if (this.equals(benefic)) {
			throw new InvalidSelfTransferException("can't do transfer to same acc. it's not an infinite money glitch");
			}
			if (balance.isLT(amt)) {
				throw new InsufficientBalanceException("insufficient funs. can't transfer. u only have: " + balance);

			}
		
		
// if transfer allowed to occur - emit 2 atomic eventz - send n recive

	record(new MoneyTransferSend(aID, benefic.getAID(), amt));
	benefic.record(new MoneyTransferReceive(benefic.getAID(), this.aID, amt));
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
		}

/*-------state mutators- earlier we only recorded---*/
//not using switch cuz not supported below jdk 16 or 17?

		private void apply(DomainEvent event) {
     		   if (event instanceof AccountOpened e) {
			 balance = e.openBal();
			} else if (event instanceof MoneyDeposited e) {
			 balance = balance.add(e.amount());
			} else if (event instanceof MoneyWithdrawn e) {
			 balance = balance.subtract(e.amount());
			} else if (event instanceof MoneyTransferSend e) {
			 balance = balance.subtract(e.amount());
			} else if (event instanceof MoneyTransferReceive e) {
			 balance = balance.add(e.amount());
        }
    }

	
	@Override
	public boolean equals(Object o) {
		return(o instanceof Account other) && aID.equals(other.aID);
	}

	@Override
	public int hashCode() { return aID.hashCode(); }
}

