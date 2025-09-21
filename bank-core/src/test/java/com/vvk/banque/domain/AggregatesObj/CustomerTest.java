package com.vvk.banque.domain.AggregatesObj;

import com.vvk.banque.domain.ValueObj.*;
import com.vvk.banque.domain.events.*;
import com.vvk.banque.domain.exceptions.*;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CustomerTest {

    private CustomerId customerId;
    private String validName;
    private String validEmail;
    private String validHashedPassword;

    @BeforeEach
    void setUp() {
        customerId = CustomerId.generate("V1K");
        validName = "Preyya Gumbr";
        validEmail = "preyya@amazon.com";
        validHashedPassword = "SyedPasssword123";
    }

    // Test 1: Sign up with valid parameters
    @Test
    void testSignUp_ValidParameters_Success() {
        Customer customer = Customer.signUp(customerId, validName, validEmail, validHashedPassword);

        assertNotNull(customer);
        assertEquals(customerId, customer.getId());
        assertEquals(validName, customer.getName());
        assertEquals(validEmail, customer.getEmail());
        assertEquals(validHashedPassword, customer.getHashedPassword());

        List<DomainEvent> uncommittedEvents = customer.getUncommittedEvents();
        assertEquals(1, uncommittedEvents.size());
        assertTrue(uncommittedEvents.get(0) instanceof CustomerSignedUp);

        CustomerSignedUp event = (CustomerSignedUp) uncommittedEvents.get(0);
        assertEquals(customerId, event.getCustomerId());
        assertEquals(validName, event.getName());
        assertEquals(validEmail, event.getEmail());
        assertEquals(validHashedPassword, event.getHashedPassword());
    }

    // Test 2: Sign up with null name
    @Test
    void testSignUp_NullName_ThrowsException() {
        assertThrows(CustomerNameException.class, 
            () -> Customer.signUp(customerId, null, validEmail, validHashedPassword));
    }

    // Test 3: Sign up with empty name
    @Test
    void testSignUp_EmptyName_ThrowsException() {
        assertThrows(CustomerNameException.class, 
            () -> Customer.signUp(customerId, "", validEmail, validHashedPassword));
    }

    // Test 4: Sign up with blank name
    @Test
    void testSignUp_BlankName_ThrowsException() {
        assertThrows(CustomerNameException.class, 
            () -> Customer.signUp(customerId, "   ", validEmail, validHashedPassword));
    }

    // Test 5: Sign up with null email
    @Test
    void testSignUp_NullEmail_ThrowsException() {
        assertThrows(EmailException.class, 
            () -> Customer.signUp(customerId, validName, null, validHashedPassword));
    }

    // Test 6: Sign up with invalid email (no @)
    @Test
    void testSignUp_InvalidEmail_ThrowsException() {
        assertThrows(EmailException.class, 
            () -> Customer.signUp(customerId, validName, "invalid-email", validHashedPassword));
    }

    // Test 7: Sign up with null password
    @Test
    void testSignUp_NullPassword_ThrowsException() {
        assertThrows(PasswordException.class, 
            () -> Customer.signUp(customerId, validName, validEmail, null));
    }

    // Test 8: Sign up with empty password
    @Test
    void testSignUp_EmptyPassword_ThrowsException() {
        assertThrows(PasswordException.class, 
            () -> Customer.signUp(customerId, validName, validEmail, ""));
    }

    // Test 9: Rebuild from history with valid events
    @Test
    void testFromHistory_WithCustomerSignedUpEvent_RebuildsStateCorrectly() {
        List<DomainEvent> history = List.of(
            new CustomerSignedUp(customerId, validName, validEmail, validHashedPassword)
        );
        
        Customer reconstructedCustomer = Customer.fromHistry(history);

        assertNotNull(reconstructedCustomer);
        assertEquals(customerId, reconstructedCustomer.getId());
        assertEquals(validName, reconstructedCustomer.getName());
        assertEquals(validEmail, reconstructedCustomer.getEmail());
        assertEquals(validHashedPassword, reconstructedCustomer.getHashedPassword());
        assertTrue(reconstructedCustomer.getUncommittedEvents().isEmpty());
    }

    // Test 10: Rebuild from history without CustomerSignedUp event
    @Test
    void testFromHistory_WithoutCustomerSignedUpEvent_ThrowsException() {
       
	 // creatin' a mock event that's not CustomerSignedUp to check correct exception thrown
        
	DomainEvent mockEvent = new DomainEvent() {
            @Override
            public String toString() {
                return "MockEvent";
            }
        };
        
        List<DomainEvent> history = List.of(mockEvent);
        
        assertThrows(CustomerNotFoundException.class, () -> Customer.fromHistry(history));
    }

    // Test 11: Test equals and hashCode
    @Test
    void testEqualsAndHashCode() {
        Customer customer1 = Customer.signUp(customerId, validName, validEmail, validHashedPassword);
        Customer customer2 = Customer.signUp(customerId, "Different Name", "different@email.com", "differentPassword");
        CustomerId differentCustomerId = CustomerId.generate("P3R");
        Customer differentCustomer = Customer.signUp(differentCustomerId, validName, validEmail, validHashedPassword);

        assertEquals(customer1, customer2); // same cid is equal
        assertNotEquals(customer1, differentCustomer);
        assertEquals(customer1.hashCode(), customer2.hashCode());
        assertNotEquals(customer1.hashCode(), differentCustomer.hashCode());
    }

    // Test 12: Test markEventsAsCommitted
    @Test
    void testMarkEventsAsCommitted() {
        Customer customer = Customer.signUp(customerId, validName, validEmail, validHashedPassword);
        assertEquals(1, customer.getUncommittedEvents().size());

        customer.markEventsAsCommitted();
        assertTrue(customer.getUncommittedEvents().isEmpty());
    }
}
