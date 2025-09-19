package com.vvk.banque.domain.ValueObj;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.assertj.core.api.Assertions.*;

class AccountIdTest {

    /* ---------- helper mock that always returns the same acc - this isn't currently there cuz wud be impl in infra, hence mock ---------- */
    private static final class FixedSeq implements DatabaseSequence {
        private final int value;
        FixedSeq(int value) { this.value = value; }
        @Override public int nextAcc() { return value; }
    }

    /* ---------- +ve cases ---------- */

    @Test
    @DisplayName("valid 5-digit number accepted")
    void validCode() {
        AccountId id = AccountId.generateUnique(new FixedSeq(12345));
        assertThat(id.getAcc()).isEqualTo(12345);
        assertThat(id.getId()).isNotNull();
    }

    @Test
    @DisplayName("same acc w/ diff uuid is allowd in vo - db enforces unique at infra")
    void sameAccDifferentUuidpass() {
        AccountId id1 = AccountId.of(12345, UUID.randomUUID());
        AccountId id2 = AccountId.of(12345, UUID.randomUUID());
        assertThat(id1).isNotEqualTo(id2); //differnt uuid on purpose
        assertThat(id1.getAcc()).isEqualTo(id2.getAcc());
    }

    /* ---------- negative cases ---------- */

    @Test
    @DisplayName("null UUID throws NPE")
    void nullUuid() {
        assertThatNullPointerException()
                .isThrownBy(() -> AccountId.of(12345, null))
                .withMessageContaining("account uid can't be null");
    }

    @Test
    @DisplayName("acc <= 0 throws IllegalArgumentException")
    void nonPositiveAcc() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> AccountId.of(0, UUID.randomUUID()));
    }

    @Test
    @DisplayName("leading zero throws IllegalArgumentException")
    void leadingZero() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> AccountId.of(1234, UUID.randomUUID())); // 
    }

    @Test
    @DisplayName("too large acc (6 digits) throws IllegalArgumentException")
    void tooManyDigits() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> AccountId.of(100_000, UUID.randomUUID()));
    }
}
