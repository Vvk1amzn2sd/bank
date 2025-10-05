package com.vvk.banque.app.ports.in;
import com.vvk.banque.app.ports.FixedSeq;

import com.vvk.banque.domain.ValueObj.AccountId;
import com.vvk.banque.domain.ValueObj.Money;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.Currency;
import static org.junit.jupiter.api.Assertions.*;

class CheckBalanceQueryTest {

@Test
void executeCheckBalance_returnsExpectedMoney() {
    AccountId id = AccountId.generateUnique(new FixedSeq());
    

    CheckBalanceQuery queryExecutor = accId -> Money.of(BigDecimal.valueOf(1000), Currency.getInstance("USD"));

    Currency appCurrency = Currency.getInstance("USD");
    
  
    Money expectedMoney = Money.of(new BigDecimal("1000.00"), appCurrency); 
    
   
    Money actualMoney = queryExecutor.executeCheckBalance(id); 

    assertEquals(expectedMoney, actualMoney);
}
}
