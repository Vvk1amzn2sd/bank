package com.vvk.banque.app.services;

import com.vvk.banque.app.ports.in.TransferMoneyCommand;
import com.vvk.banque.app.ports.out.AccountEventStorePort;
import com.vvk.banque.app.ports.out.EventPublisherPort;
import com.vvk.banque.domain.AggregatesObj.Account;
import com.vvk.banque.domain.ValueObj.AccountId;
import com.vvk.banque.domain.ValueObj.Money ;
import com.vvk.banque.domain.events.MoneyTransferSend;
import com.vvk.banque.domain.events.MoneyTransferReceive;


public class TransferMoneyCommandHandler implements TransferMoneyCommand {

      private final AccountEventStorePort eventStore	;
     private final EventPublisherPort eventPublisher	;

    public TransferMoneyCommandHandler(AccountEventStorePort eventStore
		   			, EventPublisherPort eventPublisher) {

	   this.eventStore = eventStore	;
   	   this.eventPublisher = eventPublisher	;
	}

    
	@Override
	public void executeTransferMoney(AccountId fromAccountId,
                                 AccountId toAccountId,
                                 Money     amount) {

    	Account fromAccount = eventStore.loadAccount(fromAccountId);
    	Account toAccount   = eventStore.loadAccount(toAccountId);

    	fromAccount.transferTo(toAccount, amount);

    	MoneyTransferSend    sendEvt    = new MoneyTransferSend(fromAccountId, toAccountId, amount);
    	MoneyTransferReceive receiveEvt = new MoneyTransferReceive(toAccountId, fromAccountId, amount);

    	eventStore.saveEvent(sendEvt);
    	eventStore.saveEvent(receiveEvt);

    	eventPublisher.publish(sendEvt);
    	eventPublisher.publish(receiveEvt);
	}
}
