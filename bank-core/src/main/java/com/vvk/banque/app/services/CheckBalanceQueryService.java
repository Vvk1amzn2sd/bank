package com.vvk.banque.app.services	;

import com.vvk.banque.app.ports.in.CheckBalanceQuery	;
import com.vvk.banque.app.ports.out.AccountQueryPort	;
import com.vvk.banque.domain.ValueObj.AccountId		;
import com.vvk.banque.domain.ValueObj.Money		;


public class CheckBalanceQueryService implements CheckBalanceQuery {

	//dependeency injection: the service depennd on outbound port contract
	
	private final AccountQueryPort accountQueryPort		;

	//constructr for injection

	public CheckBalanceQueryService (AccountQueryPort accountQueryPort) {
		this.accountQueryPort = accountQueryPort		;
	}


	@Override
	public Money executeCheckBalance(AccounntId accountId) {
		// the service routes the query to tht outbound port
	 // it doesnt load the acc aggregate directly to uphjold dp

	return accountQueryPort.findBalanceByAccountId(accountId)	;
	
	

	} 
}
