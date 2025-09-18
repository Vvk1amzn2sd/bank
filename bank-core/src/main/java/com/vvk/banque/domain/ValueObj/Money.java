package com.vvk.banque.domain.ValueObj;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Objects;

//This is an immutable obj - no identity but form the base

public final class Money {
	private final BigDecimal amt;
	private final Currency cur;
	
//constructr w/ caveats to enforce basic rules
	public Money(BigDecimal amt, Currency cur) {
		if (amt == null || cur == null) {
			throw new IllegalArgumentException("Monetary amount and/ or currency cant be null!");
	}
		if (amt.compareTo(BigDecimal.ZERO) < 0) {
			throw new IllegalArgumentException("Monetary value or amount must be non-negative. Money can't be negative");
	}
	this.amt = amt;
	this.cur = cur;
}

	public BigDecimal getAmt() {
		return amt;
	}
	
	public Currency getCur() {
		return cur;
	}


