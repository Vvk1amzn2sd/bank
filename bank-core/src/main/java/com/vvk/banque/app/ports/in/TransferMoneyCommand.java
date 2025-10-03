package com.vvk.banque.app.ports.in;

import com.vvk.banque.domain.ValueObj.AccountId	;
import com.vvk.banque.domain.ValueObj.Money	;

public interface TransferMoneyCommand {

	void executeTransferMoney(AccountId fromAccountId
				, AccountId toAccountId
				, Money amount);

}
