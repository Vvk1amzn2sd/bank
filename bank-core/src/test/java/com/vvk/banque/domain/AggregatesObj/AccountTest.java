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

// Test2 passing -ve  balance exception- seccond line of defence, first one lives in domain where money vo, monies cnt be -ve
	@Test
	void test2_OpenAccount_NegativeOpeningBalance_ThrowsException() {
		Money negativeBalance = Money.of(new BigDecimal("-100.0"), Currency.getInstance("INR"));
		assertThrows(OpeningBalanceNullException.class, () -> Account.open(accId, ownId, negativeBalance));
		} 
		

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
}




	
