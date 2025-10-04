package com.vvk.banque.domain.ValueObj;

import java.util.Objects;
import java.util.UUID;

public final class AccountId {

    private final int    acc;      // 5-digit numeric part
    private final UUID   id;       // real identifier

    /* ---------- constructor ---------- */
    public AccountId(int acc, UUID id) {
        if (acc <= 0)  throw new IllegalArgumentException("AccountId must be positive");
        String padded = String.format("%05d", acc);
        if (padded.charAt(0) == '0')
            throw new IllegalArgumentException("First digit of accID can't be zero");
        this.acc = acc;
        this.id  = Objects.requireNonNull(id, "account uid can't be null");
    }

    /* ---------- factory for infra layer ---------- */
    public static AccountId generateUnique(DatabaseSequence seq) {
        return new AccountId(seq.nextAcc(), UUID.randomUUID());
    }

    /* ---------- getters ---------- */
    public int  getAcc() { return acc; }
    public UUID getId()  { return id;  }

    /* ---------- value-object equality (UUID only) ---------- */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AccountId)) return false;
        return id.equals(((AccountId) o).id);
    }
    @Override
    public int hashCode() { return id.hashCode(); }

    /* ---------- ALWAYS emit full UUID so CLI can round-trip(changed to this while testing demo cli will tag change-round-trip-cli for commit ---------- */
    @Override
    public String toString() {
        return String.format("%05d-%s", acc, id);   // 5-digit-full-UUID
    }

    /* ---------- rebuild from the same string ---------- */
    public static AccountId fromString(String raw) {
        int dash = raw.indexOf('-');
        if (dash == -1) throw new IllegalArgumentException("Bad AccountId format");
        int   num = Integer.parseInt(raw.substring(0, dash));
        UUID uuid = UUID.fromString(raw.substring(dash + 1));
        return new AccountId(num, uuid);
    }
}
