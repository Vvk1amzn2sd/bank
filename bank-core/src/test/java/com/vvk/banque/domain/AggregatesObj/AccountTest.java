package com.vvk.banque.domain.AggregatesObj;

import com.vvk.banque.domain.ValueObj.*;
import com.vvk.banque.domain.events.*;
import com.vvk.banque.domain.exceptions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.math.BigDecimal;
import java.util.Currency;

import static org.junit.jupiter.api.Assertions.*;
import static org.assertj.core.api.Assertions.assertThat;

class AccountTest {

	private AccountId accId;
	private AccountId beneficiaryId;
	private CustomerId ownId;
	private Money initialBal;
	private Account account;

	// mock dbseq for gen test acc no.
	private final DatabaseSequence mockDbSeq = new DatabaseSequence () {
		private int acc = 10100;
		@Override
		public int nextAcc() {
			return acc++;
		}
	}; 	

	@BeforeEach
	void setUp() {
		accId	 	= AccountId.generateUnique(mockDbSeq);
		beneficiaryId 	= AccountId.generateUnique(mockDbSeq);
		ownId		= CustomerId.generate("VVK");
		initialBal	= Money.of(new BigDecimal("100.0"), Currency.getInstance("INR"));
		account		= Account.open(accId, ownId, initialBal);
		} 

	// -- Account.open() Tests --

/*--- testing static open() factory -> correctly creates a new Account Object when valid non-negative opening bal
	make sure accId, ownId, initialBal set correctly + single AccountOpened event is recorded in uncommitted event lis----*/	

	@Test
	void test1_OpenAccount_ValidOpeningBalance_Success() {
		assertNotNull(account);
		assertEquals(accId, account.getAID());
		assertEquals(ownId, account.getOwnerId());
		assertEquals(initialBal, account.getBalance());
		
		List<DomainEvent> uncommittedEvents = account.getUncommittedEvents();
		assertEquals(1, uncommittedEvents.size());
		assertTrue(uncommittedEvents.get(0) instanceof AccountOpened);

		AccountOpened event = (AccountOpened) uncommittedEvents.get(0);
		assertEquals(accId, event.getAccountId());
		assertEquals(ownId, event.getOwnerId());
		assertEquals(initialBal, event.getOpenBal());
		}
/*---commenting this test after succesful testing, will get rid of it, once i allow credit, overdraw on acc
// Test2 passing -ve  balance exception- seccond line of defence, first one lives in domain where money vo, monies cnt be -ve
	@Test
	void test2_OpenAccount_NegativeOpeningBalance_ThrowsException() {
		Money negativeBalance = Money.of(new BigDecimal("-100.0"), Currency.getInstance("INR"));
		assertThrows(OpeningBalanceNullException.class, () -> Account.open(accId, ownId, negativeBalance));
		} 
----------*/		

//Test3 - passing null balance as staring
	
	@Test
	void test3_OpenAccount_NullOpeningBalance_ThrowsException() {
		assertThrows(OpeningBalanceNullException.class, () -> Account.open(accId, ownId, null));
		}

//Test4 -- Account.fromHistry() Tests ----

	@Test
	void testFromHistry_WithAccountOpenedEvent_RebuildsStateCorrectly() {
		Money demo_start_bal = Money.of(new BigDecimal("100.0"), Currency.getInstance("INR"));
		List<DomainEvent> history = List.of(new AccountOpened(accId, ownId, demo_start_bal));
		Account reconstructedAcc  = Account.fromHistry(accId, history);

		assertNotNull(reconstructedAcc);
		assertEquals(accId, reconstructedAcc.getAID());
		assertEquals(ownId, reconstructedAcc.getOwnerId()); 	
		assertEquals(initialBal, reconstructedAcc.getBalance());
		assertTrue(reconstructedAcc.getUncommittedEvents().isEmpty());
		}

//Test5 -- withdrawl/deposit rebuild state check
	
	@Test
	void testFromHistry_WithDepositsAndWithdrawals_RebuildsStateCorrectly() {
		Money demo_start_bal = Money.of(new BigDecimal("100.0"), Currency.getInstance("INR"));
		Money demo_deposit   = Money.of(new BigDecimal("100.0"), Currency.getInstance("INR"));
		Money demo_withdraw  = Money.of(new BigDecimal("169.0"), Currency.getInstance("INR"));
		List<DomainEvent> history = List.of( new AccountOpened(accId, ownId, demo_start_bal)
							,new MoneyDeposited(accId, demo_deposit)
							,new MoneyWithdrawn(accId, demo_withdraw)
						    );
		Account recnstructedAccount = Account.fromHistry(accId, history);

		assertNotNull(recnstructedAccount);
		assertEquals(Money.of(new BigDecimal("31.0"), Currency.getInstance("INR")),  recnstructedAccount.getBalance());
		assertTrue(recnstructedAccount.getUncommittedEvents().isEmpty());
		}
//Test 6 -- account open exception test

	@Test
	void testFromHistory_WithoutAccountOpenedEvent_ThrowsException() {
	Money demo_deposit   = Money.of(new BigDecimal("100.0"), Currency.getInstance("INR"));      
 	List<DomainEvent> history = List.of(new MoneyDeposited(accId, demo_deposit));
        assertThrows(AccountOpenedException.class, () -> Account.fromHistry(accId, history));
    }


//Test 7 -- deposit method inc balance & records MoneyDeposited Event , both check finalstate i.e. balance & event log

	@Test
	void testDeposit_ValidAmount_UpdatesBalanceAndRecordsEvent() {
	
	Money demo_deposit   = Money.of(new BigDecimal("100.0"), Currency.getInstance("INR"));
	

        account.deposit(demo_deposit);
        assertEquals(Money.of(new BigDecimal("200.0"), Currency.getInstance("INR")), account.getBalance());

        List<DomainEvent> uncommittedEvents = account.getUncommittedEvents();
        assertEquals(2, uncommittedEvents.size());
        assertTrue(uncommittedEvents.get(1) instanceof MoneyDeposited);

	        
        MoneyDeposited event = (MoneyDeposited) uncommittedEvents.get(1);
        assertEquals(accId, event.accountId());
        assertEquals(demo_deposit,  event.amount());
	assertEquals(Money.of(new BigDecimal("200.0"), Currency.getInstance("INR")), account.getBalance());
    
}

//Test8 - zero amt thrws exception

	@Test
	void testDeposit_ZeroAmount_ThrowsException() {
    	assertThrows(PositiveMoneyException.class,() -> account.deposit(Money.of(new BigDecimal("0.0"), Currency.getInstance("INR"))));
}

/*---commenting out all -ve money here after successful test since money VO enforces it and junit will be all green post this

//Test 9 - negative amt deposit thrw exceptino 

	@Test
	void testDeposit_NegativeAmount_ThrowsException() {
    	assertThrows(PositiveMoneyException.class,
            () -> account.deposit(Money.of(new BigDecimal("-10.0"), Currency.getInstance("INR"))));
}
----*/
/*-----withdrawals below ----*/

//Test 10 - withdraw valid amt, update balance and record

	@Test
	void testWithdraw_ValidAmount_UpdatesBalanceAndRecordsEvent() {
    	Money withdraw = Money.of(new BigDecimal("50.0"), Currency.getInstance("INR"));
    	account.withdraw(withdraw);

    	assertEquals(Money.of(new BigDecimal("50.0"), Currency.getInstance("INR")), account.getBalance());

    	List<DomainEvent> events = account.getUncommittedEvents();
    	assertEquals(2, events.size());
    	assertTrue(events.get(1) instanceof MoneyWithdrawn);

    	MoneyWithdrawn event = (MoneyWithdrawn) events.get(1);
    	assertEquals(accId, event.accountId());
    	assertEquals(withdraw, event.amount());
}

//Test 11 - insuffienctient balcne exception
	
	@Test
	void testWithdraw_InsufficientBalance_ThrowsException() {
    	Money overdraft = Money.of(new BigDecimal("150"), Currency.getInstance("INR"));
    	assertThrows(InsufficientBalanceException.class, () -> account.withdraw(overdraft));
}

//Test12 - withdrw zerio amt

	@Test	
	void testWithdraw_ZeroAmount_ThrowsException() {
    	assertThrows(PositiveMoneyException.class,
            () -> account.withdraw(Money.of(new BigDecimal("0.0"), Currency.getInstance("INR"))));
}


/*-------- -ve 
//Test13 - wdaw -ve amt

	@Test
	void testWithdraw_NegativeAmount_ThrowsException() {
    	assertThrows(PositiveMoneyException.class,
            () -> account.withdraw(Money.of(new BigDecimal("-10.0"), Currency.getInstance("INR"))));
}
----*/

/* --- Transfer tests ---*/

//Test14 - valid trnsfer, updatebalance, record

	@Test
	void testTransferTo_ValidTransfer_UpdatesBalancesAndRecordsEvents() {
    	Account beneficiary = Account.open(beneficiaryId,
            CustomerId.generate("V1K"), Money.of(new BigDecimal("200"), Currency.getInstance("INR")));

    	Money transfer = Money.of(new BigDecimal("50"), Currency.getInstance("INR"));
    	account.transferTo(beneficiary, transfer);

    assertEquals(Money.of(new BigDecimal("50"), Currency.getInstance("INR")), account.getBalance());
    assertEquals(Money.of(new BigDecimal("250"), Currency.getInstance("INR")), beneficiary.getBalance());

    List<DomainEvent> senderEvents = account.getUncommittedEvents();
    assertEquals(2, senderEvents.size());
    assertTrue(senderEvents.get(1) instanceof MoneyTransferSend);
    MoneyTransferSend send = (MoneyTransferSend) senderEvents.get(1);
    assertEquals(accId, send.fromAccountId());
    assertEquals(beneficiaryId, send.toAccountId());
    assertEquals(transfer, send.amount());

    List<DomainEvent> benEvents = beneficiary.getUncommittedEvents();
    assertEquals(2, benEvents.size());
    assertTrue(benEvents.get(1) instanceof MoneyTransferReceive);
    MoneyTransferReceive recv = (MoneyTransferReceive) benEvents.get(1);
    assertEquals(beneficiaryId, recv.toAccountId());
    assertEquals(accId, recv.fromAccountId());
    assertEquals(transfer, recv.amount());
}


//Test15 - tranfer to NUll exception, trivial tests below, unnamed (tired of namin)


@Test
void testTransferTo_NullBeneficiary_ThrowsException() {
    assertThrows(BeneficiaryAccountNullException.class,
            () -> account.transferTo(null, Money.of(new BigDecimal("10"), Currency.getInstance("INR"))));
}


@Test
void testTransferTo_SelfTransfer_ThrowsException() {
    assertThrows(InvalidSelfTransferException.class,
            () -> account.transferTo(account, Money.of(new BigDecimal("10"), Currency.getInstance("INR"))));
}

@Test
void testTransferTo_InsufficientBalance_ThrowsException() {
    Account beneficiary = Account.open(beneficiaryId,
            CustomerId.generate("K1M"), Money.of(new BigDecimal("200"), Currency.getInstance("INR")));
    Money over = Money.of(new BigDecimal("150"), Currency.getInstance("INR"));
    assertThrows(InsufficientBalanceException.class, () -> account.transferTo(beneficiary, over));
}

@Test
void testTransferTo_ZeroAmount_ThrowsException() {
    Account beneficiary = Account.open(beneficiaryId,
            CustomerId.generate("V1K"), Money.of(new BigDecimal("200"), Currency.getInstance("INR")));
    assertThrows(PositiveMoneyException.class,
            () -> account.transferTo(beneficiary, Money.of(new BigDecimal("0"), Currency.getInstance("INR"))));
}



/*----- -ve
@Test
void testTransferTo_NegativeAmount_ThrowsException() {
    Account beneficiary = Account.open(beneficiaryId,
            CustomerId.generate("VK1"), Money.of(new BigDecimal("200"), Currency.getInstance("INR")));
    assertThrows(PositiveMoneyException.class,
            () -> account.transferTo(beneficiary, Money.of(new BigDecimal("-10"), Currency.getInstance("INR"))));
}
------*/

// --- Other Tests ---
@Test
void testEqualsAndHashCode() {
    Account same = Account.open(accId, ownId, Money.of(new BigDecimal("200"), Currency.getInstance("INR")));
    Account different = Account.open(beneficiaryId, ownId, Money.of(new BigDecimal("100"), Currency.getInstance("INR")));

    assertEquals(account, same);
    assertNotEquals(account, different);
    assertEquals(account.hashCode(), same.hashCode());
    assertNotEquals(account.hashCode(), different.hashCode());
}

@Test
void testGetUncommittedEvents() {
    assertEquals(1, account.getUncommittedEvents().size());
    assertTrue(account.getUncommittedEvents().get(0) instanceof AccountOpened);

    account.deposit(Money.of(new BigDecimal("10"), Currency.getInstance("INR")));
    assertEquals(2, account.getUncommittedEvents().size());
    assertTrue(account.getUncommittedEvents().get(1) instanceof MoneyDeposited);
}

@Test
void testMarkEventsAsCommitted() {
    account.deposit(Money.of(new BigDecimal("10"), Currency.getInstance("INR")));
    assertEquals(2, account.getUncommittedEvents().size());

    account.markEventsAsCommitted();
    assertTrue(account.getUncommittedEvents().isEmpty());
}

@Test
void test_read_only_bal_queryShit(){
	assertThat(account.read_only_qury_balance())
	.isEqualTo(Money.of(new BigDecimal("100"), Currency.getInstance("INR")));
	} 

}




	
