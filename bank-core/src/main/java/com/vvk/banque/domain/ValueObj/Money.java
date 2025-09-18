package com.vvk.banque.domain.ValueObj;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Objects;

//This represents monetary value, simple numbers mathematically, but inherent value economically - no identity yet immutable(for ts & predictability)
// VO is self-validating, always in valid state, once created, much like money irl which will always find wanters
public final class Money {
	private final BigDecimal amt;
	private final Currency cur;
	
//constructr w/ caveats to enforce basic rules
	public Money(BigDecimal amt, Currency cur) {
// guardrail: validate state before setting cosntructor initial value 
		if (amt == null || cur == null) {
			throw new IllegalArgumentException("Monetary amount and currency cant be null!");
	}
		if (amt.compareTo(BigDecimal.ZERO) < 0) {
			throw new IllegalArgumentException("Monetary value or amount must be non-negative. Money can't be negative");
	}
	this.amt = amt;
	this.cur = cur;
}
//getters - to provide read access since amt & cur themselves are marked final
	public BigDecimal getAmt() {
		return amt;
	}
	
	public Currency getCur() {
		return cur;
	}

// BL: defining what to do w/ money - add(credit), subtract(withdraw), both - transfer?

	public Money add(Money other) {
		MatchCurrency(other);	// checking currency match prior to adding
		return new Money(this.amt.add(other.amt), this.cur);
	}
	
	public Money subtract(Money other) {
		MatchCurrency(other);
		return new Money(this.amt.subtract(other.amt), this.cur);
	}

	public boolean amtCompare (Money other) {
		MatchCurrency(other);
		return this.amt.compareTo(other.amt) < );
	}

// This helper method is for strictly ensuring  matching currency already used above
	private void MatchCurrency(Money other) {
		if (!this.cur.equals(other.cur)) {
			throw new IllegalArgumentException("currencies do not match. Make sure they match in order to proceed");
	}
}












