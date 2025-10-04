package com.vvk.banque.domain.ValueObj;

import java.util.Objects;
import java.util.UUID;

public final class AccountId {

    private final int    acc;      // 5 digit acc id, unique to customer but no identifier, used uuid as identifier
    private final UUID   id;       // real identity


    public AccountId(int acc, UUID id) {
        if (acc <= 0) {
            throw new IllegalArgumentException("AccountId must be positive");
        }

        String padded = String.format("%05d", acc);

        if (padded.length() != 5) {                       // safety net in extreme
            throw new IllegalArgumentException("AccountId must be exactly 5 digits");
        }

        if (padded.charAt(0) == '0') {
            throw new IllegalArgumentException("First digit of accID can't be zero");
        }

        this.acc = acc;
        this.id  = Objects.requireNonNull(id, "account uid can't be null");
    }

	/*---pkg-pvt --*/

	 static AccountId of(int acc, UUID id) {
		return new AccountId(acc, id);
	}

    /* ---------- factory ---------- */

    public static AccountId generateUnique(DatabaseSequence seq) {
	int acc = seq.nextAcc(); // this purely to keep 5 digit acc unique - will be impl in infra	
        return new AccountId(acc, UUID.randomUUID());
    }

    /* ---------- getters ---------- */
    public int  getAcc() { return acc; }
    public UUID getId()  { return id;  }

    /* ---------- value-object identity (UUID only) ---------- */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AccountId)) return false;
        AccountId other = (AccountId) o;
        return id.equals(other.id);
    }

    @Override
    public int hashCode() { return id.hashCode(); }

    @Override
    public String toString() { return String.format("%05d-%s", acc, id); }
}
