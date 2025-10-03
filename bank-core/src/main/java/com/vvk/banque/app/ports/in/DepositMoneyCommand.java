package com.vvk.banque.app.ports.in;

import com.vvk.banque.domain.ValueObj.AccountId	;
import com.vvk.banque.domain.ValueObj.Money	;

public interface DepositMoneyCommand {

   void executeDepositMoney(AccountId accountId, Money amount);

}
