package com.vvk.banque.domain.ValueObj;

import java.util.Objects;
import java.util.UUID;

public final class CustomerId {
   
	 private final String cust; // cust can pick same cust - irl 2 ppl can share same name
	 private final UUID cust_uuid;    	// unique identifier

// constr - only place where state is allowed to b injectd

    public CustomerId(String cust, UUID cust_uuid) {
	
        if (cust == null || cust.length() != 3 || !cust.matches("[A-Z0-9]{3}")) {
            throw new IllegalArgumentException("Customer id must be exactly 3 alphanumeric characters");
        }
        if (Character.isDigit(cust.charAt(0))) {
            throw new IllegalArgumentException("First character of cust id cannot be numeric");
        }
        this.cust = cust;
	this.cust_uuid = Objects.requireNonNull(cust_uuid, "customer uid can't be null");
    }

	/*--factories---*/

	public static CustomerId generate (String cust) {
		return new CustomerId(cust, UUID.randomUUID());
	}

/*---getters---*/

    public UUID getCust_uuid() { return cust_uuid; }

    public String getCust() { return cust; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CustomerId)) return false;
        CustomerId other = (CustomerId) o;
        return cust_uuid.equals(other.cust_uuid);         
    }

    @Override
    public int hashCode() { return Objects.hash(cust_uuid); }

    @Override
    public String toString() { return cust + "-" + cust_uuid.toString(); }

}
