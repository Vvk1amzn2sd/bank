package com.vvk.banque.domain.ValueObj;

import java.util.Objects;

public final class CustomerId {
    private final String cust;

    public CustomerId(String cust) {
        if (cust == null || cust.length() != 3 || !cust.matches("[A-Z0-9]{3}")) {
            throw new IllegalArgumentException("Customer id must be exactly 3 alphanumeric characters");
        }
        if (Character.isDigit(cust.charAt(0))) {
            throw new IllegalArgumentException("First character of cust id cannot be numeric");
        }
        this.cust = cust;
    }

    public String getCust() { return cust; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CustomerId)) return false;
        CustomerId that = (CustomerId) o;
        return cust.equals(that.cust);          
    }

    @Override
    public int hashCode() { return Objects.hash(cust); }

    @Override
    public String toString() { return cust; }

}

