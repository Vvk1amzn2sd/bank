package com.vvk.banque.domain.AggregatesObj;

import com.vvk.banque.domain.ValueObj.*;
import com.vvk.banque.domain.events.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class Account {

	private final	AccountId	aID;		
	private final	CustomerId	ownerId;		// only single acc now, might update in future to rflect joint acc util list
	private		Money		balance;	
	
/*----event-sourcing logging intended, hence account here is pure state m/c for emitting domain events---*/

	private final List<DomainEvent> uncommited = new ArrayList<>();

/*----factory for new account---*/

	public static Account open(AccountId 	accId,
				   CustomerId 	ownId,
				   Money	openBal) {

		if (openBal == null || openBal.isNegative()) {
			throw new IllegalArgumentException("Opening balance must be non-negative i.e. 0 or more");
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
			Account acc = new Account(id, null, null); // owner & bal rebuilt
				history.forEach(acc::apply);
					return acc;
			}

	// pvt cnstrctr
	private Account(AccountId aID, CustomerId ownerId, Money bal) {
		this.aID	= aID;
		this.cID	= ownerId;
		this.balance	= bal;
	}

	//getters

	public get AID() { return aID; }
	public get OwnerId() { return OwnerId; }
	public get Balance() { return balance; }
	
	public List<DomainEvent> getUncommittedEvents() {
		return Collections.unmodifiableList(uncommitted); 
		}
	
	public void markEventAsCommitted() { uncommitted.clear(); }
	
	
/*-----defining vehavior below - this is not a usecase still, this is raw behvior, much like domain is suppose to be--*/

	/*---deposits---*/
	
	public void deposit(Money amt) {
		ensurePositive(amt);
		balance = balance.add(amt);
	}

	/*---withdrawals----*/

	public void withdraw(Money amt) {
		ensurePositive(amt);
		if (balance.isLT(amt)) {
			throw new InsufficientBalanceException("insufficient funds. oops! you currently only have" + balance +"in ur acc");
			}
		balance = balance.subtract(amt);
	}

	/*---transfers----*/

	public Transaction transferto(Account benefic, Money amt) {
		
	//cant transfer to same acc

		if (this.equals(benefic)) {
			throw new InvalidSelfTransferException("can't do transfer to same acc. it's not an infinite money glitch");
			}
			if (balance.isLT(amt)) {
				throw new InsufficientBalanceException("insufficient funs. can't transfer. u only have" + balance);

			}
		ensurePositive(amt);
		
// if transfer allowed to occur - emit 2 atomic events, 

	record(new MoneyTransferSend(id, benefic.getId(), amt));
	benefic.record(new MoneyTransferReceive(benefic.getId(), id, amt);
	}

/*---helpers---*/

	private static void ensurePositive(Money m) {
		if(!m.isPositive()) {
			throw new IllegalArgumentException("amt must be more than 0");
			}

	private void record(DomainEvent event) {
		apply(event);
		uncommitted.add(event);
		}

/*-------state mutators---*/

		private void apply(DomainEvent event) {
			if (event instanceof AccountOpened e) {
		 	balance	=	e.openBal();
		} else if(event instanceOf MoneyDeposited e) {
			balance =	balance.add(e.amt());
		} else if(event instanceOf MoneyTransferSend e) {
			balance =	balance.subtract(e.amt());
		} else if(event instanceOf MoneyTransferReceive e) {
			balance =	balance.add(e.amt());
		}
/* leaving space for fwd compatibility - will define new events later    */
}
	
	@Override
	public boolean equals(Object o) {
		return(o instanceOf Account other) && id.equals(other.id);
	}

	@Override
	public int hashCode() ( return id.hashCode(); }
}

