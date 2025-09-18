package com.vvk.banque.domain.ValueObj;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

//This represents monetary value, simple numbers mathematically, but inherent value economically - no identity yet immutable(for ts & predictability)
// VO is self-validating, always in valid state, once created, much like money irl which will always find wanters
public final class Money implements Comparable<Money> {
	private final BigDecimal amt;
	private final Currency cur;

/* zero cache -- sinle shared 0 starting balance instance rather than having thousands of identical objects */

	private static final ConcurrentMap<Currency, Money> ZEROS = new ConcurrentHashMap<>();
	
//constructr w/ caveats to enforce basic rules

	public Money(BigDecimal amt, Currency cur) {
// guardrail: validate state before setting cosntructor initial value 
		if (amt == null || cur == null) {
			throw new IllegalArgumentException("Monetary amount and currency cant be null!");
	}
		if (amt.signum() < 0) {
			throw new IllegalArgumentException("moniees can't be negative!");
	}
	// to avoid -ve zero

	this.amt = amt.stripTrailingZeros()                      
              		.setScale(cur.getDefaultFractionDigits(), RoundingMode.HALF_EVEN);    // rounding to nearest even
	this.cur = cur;
}

/*--factories added, to make returning new object clearer--*/
	public static Money of(BigDecimal amount, Currency currency) {
		return new Money(amount, currency);
	}

	public static Money zero(Currency currency)	{
		return ZEROS.computeIfAbsent(currency, c -> new Money(BigDecimal.ZERO, c));
	}

// getters
	public BigDecimal getAmt() {
		return amt;
	}
	
	public Currency getCur() {
		return cur;
	}

	/*- quick tests to enforce */
	
	public boolean isZero()	    { return amt.signum() == 0; }
	public boolean isPositive() { return amt.signum() > 0; }
	public boolean isNegative() { return amt.signum() < 0; }

// BL loosely:  defining what to do w/ money 

	public Money add(Money other) {
		matchCurrency(other);	// checking currency match prior to adding
		return new Money(this.amt.add(other.amt), this.cur);	// new Money obj
	}
	
	public Money subtract(Money other) {
		matchCurrency(other);
		return new Money(this.amt.subtract(other.amt), this.cur);
	}
	
	public Money multiply (BigDecimal factor, RoundingMode mode) {
		return new Money(
			this.amt.multiply(factor)
				 .setScale(cur.getDefaultFractionDigits(), mode), this.cur);
	}

	public boolean isLT(Money other) {
		matchCurrency(other);
		return this.amt.compareTo(other.amt) < 0;
	}
	
	/*--comparable--*/
	@Override
	public int compareTo(Money other) {
		matchCurrency(other);
		return this.amt.compareTo(other.amt);
	}

/*-- this helper method is for strictly ensuring  matching currency, used above --*/
	private void matchCurrency(Money other) {
		if (!this.cur.equals(other.cur)) {
			throw new IllegalArgumentException("currencies do not match: " + this.cur + " != " + other.cur);
	}
}

//ovveriding equal -  value and hashcode - enforcing equality by value sheerly if both quantities are money, not depending on meomroy add

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;	// if same object its equal, not depending on memory
		if (!(o instanceof Money)) return false; // if it is not a money object, its not equal obviously
		Money money = (Money) o; //cast the object- typecasting
	 return amt.equals(money.amt) && cur.equals(money.cur);
}
	
	@Override
	public int hashCode() {
		return Objects.hash(amt, cur);
	}

	@Override
	public String toString() {
		return amt + " " + cur.getCurrencyCode();
		 }
}











