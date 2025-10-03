package com.vvk.banque.app.services	;

import com.vvk.banque.app.ports.in.DepositMoneyCommand	;
import com.vvk.banque.app.ports.out.AccountEventStorePort;
import com.vvk.banque.app.ports.out.EventPublisherPort	;
import com.vvk.banque.domain.ValueObj.AccountId		;
import com.vvk.banque.domain.ValueObj.Money		;
import com.vvk.banque.domain.events.MoneyDeposited	;
import com.vvk.banque.domain.AggregatesObj.Account	;

public class DepositMoneyCommandHandler implements DepositMoneyCommand {

	private final AccountEventStorePort eventStore	;
       private final EventPublisherPort	eventPublisher	;

	public DepositMoneyCommandHandler ( AccountEventStorePort eventStore
		 			    , EventPublisherPort eventPublisher ) {
		this.eventStore = eventStore	;
		this.eventPublisher = eventPublisher ;
	}	


           @Override
         public void executeDepositMoney(AccountId accountId, Money amount) {

	/*---1. hydrate aggregte --*/

	Account account = eventStore.loadAccount(accountId)	;

	/*--2. muatate ---------*/

	account.deposit(amount)	;

	/*----3. persiste new event ----*/

	MoneyDeposited event = new MoneyDeposited(accountId, amount)	;
	eventStore.saveEvent(event)	;

	/*----4. publish to outside wrld-----*/

	eventPublisher.publish(event)	;



	}
}
