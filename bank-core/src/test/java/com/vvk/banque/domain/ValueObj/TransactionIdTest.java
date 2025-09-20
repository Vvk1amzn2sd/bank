package com.vvk.banque.domain.ValueObj;

import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class TransactionIdTest {

    @Test
    void equalityWhenSameUuid() {
        UUID uuid = UUID.randomUUID();
        TransactionId id1 = new TransactionId(uuid);
        TransactionId id2 = new TransactionId(uuid);

        assertEquals(id1, id2);
        assertEquals(id1.hashCode(), id2.hashCode());
    }

    @Test
    void inequalityWhenDifferentUuid() {
        TransactionId id1 = new TransactionId(UUID.randomUUID());
        TransactionId id2 = new TransactionId(UUID.randomUUID());

        assertNotEquals(id1, id2);
    }

    @Test
    void notEqualToNullOrDifferentType() {
        TransactionId id = new TransactionId(UUID.randomUUID());

        assertNotEquals(id, null);
        assertNotEquals(id, new Object());
    }

    @Test
    void constructorRejectsNull() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> new TransactionId(null) );
        assertEquals("The txn id can never be null!", ex.getMessage());
    }
}

