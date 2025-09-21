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
		private int acc = 10000;
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
		initialBal	= Money.of(new BigDecimal("100.0"), Currency.getInstance("USD"));
		account		= Account.open(accId, ownId, initialBal);
		} 

	// -- Account.open() Tests --
	
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

	@Test
	void test2_OpenAccount_NegativeOpeningBalance_ThrowsException() {
		Money negativeBalance = Money.of(new BigDecimal("-50.0"), Currency.getInstance("USD"));
		assertThrows(OpeningBalanceNullException.class, () -> Account.open(accId, ownId, negativeBalance));
		} 
}
