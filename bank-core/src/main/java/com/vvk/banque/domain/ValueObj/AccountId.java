package com.vvk.banque.domain.ValueObj;

import java.util.Objects;

public final class AccountId {
	private final int acc;

	public AccountId(int acc) {
		if (acc <= 0) {
			throw new IllegalArgumentException("AccountId must be positive");
		}
	
	String padded = String.format("%05d", acc);
		if (padded.length() != 5) {
			throw new IllegalArgumentException("AccountId must be exactly 5 digits");
		}
			if (padded.charAt(0) == '0') {
				throw new IllegalArgumentException("First digit of accID can't be zero");
		}
			
			this.acc = acc;
	}

	//getter\
	
	public int getAcc() {
		return acc;
	}

	@Override
	public boolean equals(Object o) {
		if (this ==o) return true;
		if (! (o instanceof AccountId)) return false;
		AccountId that = (AccountId) o;
		return acc == that.acc;
	}

	@Override
	public int hashCode() {
		return Objects.hash(acc);
		}

	@Override
	public String toString() {
		return String.valueOf(acc);
		}
} 
		
 
