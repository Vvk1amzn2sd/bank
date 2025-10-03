package com.vvk.banque.app.services;

import com.vvk.banque.app.ports.in.WithdrawMoneyCommand;
import com.vvk.banque.app.ports.out.AccountEventStorePort;
import com.vvk.banque.app.ports.out.EventPublisherPort;

import com.vvk.banque.domain.AggregatesObj.Account;
import com.vvk.banque.domain.ValueObj.AccountId;
import com.vvk.banque.domain.ValueObj.Money;
import com.vvk.banque.domain.events.MoneyWithdrawn;

public class WithdrawMoneyCommandHandler implements WithdrawMoneyCommand {

	private final AccountEventStorePort eventStore	;
	private final EventPublisherPort    eventPublisher	;

	public WithdrawMoneyCommandHandler( AccountEventStorePort eventStore, 
						EventPublisherPort eventPublisher ) {
			
			this.eventStore	    =	eventStore	;
			this.eventPublisher =	eventPublisher	;
	}

 
               @Override
		public void executeWithdrawMoney(AccountId accountId, Money amount) {
		
		/*---1.hydrate aggregate----*/

		Account account	=	eventStore.loadAccount (accountId)	;

		/*---2.mutate--------------*/
		
		account.withdraw(amount)	;
		

		/*---3.persist new evennt---*/

		MoneyWithdrawn event = new MoneyWithdrawn(accountId, amount)	;
		eventStore.saveEvent(event)	;

		/*----4.publish to outside wrld---*/

		eventPublisher.publish(event)		;
	}
}

