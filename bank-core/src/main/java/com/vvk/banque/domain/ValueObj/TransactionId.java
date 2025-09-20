package com.vvk.banque.domain.ValueObj;

import java.util.Objects;
import java.util.UUID;

public final class TransactionId {
	private final UUID	tID;
	
	public TransactionId(UUID tID) {
	
	if (tID == null) {
		throw new IllegalArgumentException("The txn id can never be null!");
		}

	this.tID=tID;

	}

	/*---factory---*/

	public static TransactionId generate() {
		return new TransactionId(UUID.randomUUID());
		}

	// getter

	public UUID getTID() {

		return tID;
	
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) { return true; }
	
	if(!(o instanceof TransactionId)) { return false; }

	TransactionId that = (TransactionId) o ;
		return tID.equals(that.tID);
	  }

	@Override
	public int hashCode() {
		return Objects.hash(tID);
	  }

	@Override
	public String toString() {
		return tID.toString();
	  }

}
