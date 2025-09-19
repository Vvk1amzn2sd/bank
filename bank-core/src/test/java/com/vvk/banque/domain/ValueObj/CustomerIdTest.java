package com.vvk.banque.domain.ValueObj;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.assertj.core.api.Assertions.*;

class CustomerIdTest {

    @Test
    @DisplayName("valid 3-letter code accepted")
    void validCode() {
        CustomerId id = CustomerId.generate("ABC");
        assertThat(id.getCust()).isEqualTo("ABC");
        assertThat(id.getCust_uuid()).isNotNull();
    }

    @Test
    @DisplayName("duplicate cust codes are allowed if UUID differs")
    void duplicateCustOk() {
        CustomerId id1 = CustomerId.generate("ABC");
        CustomerId id2 = CustomerId.generate("ABC");
        assertThat(id1).isNotEqualTo(id2);          // unique UUID
        assertThat(id1.getCust()).isEqualTo(id2.getCust());
    }

    @Test
    @DisplayName("same UUID means same identity")
    void sameUuidMeansEqual() {
        UUID uuid = UUID.randomUUID();
        CustomerId id1 = new CustomerId("ABC", uuid);
        CustomerId id2 = new CustomerId("XYZ", uuid);
        assertThat(id1).isEqualTo(id2);             // uuid drives equality
        assertThat(id1.getCust()).isNotEqualTo(id2.getCust());
    }

    @Test
    @DisplayName("null UUID throws NPE")
    void nullUuid() {
        assertThatNullPointerException()
                .isThrownBy(() -> new CustomerId("ABC", null))
                .withMessage("customer uid can't be null");
    }

    @Test
    @DisplayName("invalid cust codes throw IllegalArgumentException")
    void invalidCustCodes() {
        assertThatIllegalArgumentException().isThrownBy(() -> CustomerId.generate("AB"));   // too shrt
        assertThatIllegalArgumentException().isThrownBy(() -> CustomerId.generate("1BC"));  // digit first
        assertThatIllegalArgumentException().isThrownBy(() -> CustomerId.generate("abc"));  // lower-case
    }
}
