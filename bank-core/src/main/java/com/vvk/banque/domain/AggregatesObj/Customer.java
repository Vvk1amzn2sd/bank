package com.vvk.banque.domain.AggregatesObj;

import com.vvk.banque.domain.ValueObj.CustomerId;
import com.vvk.banque.domain.events.CustomerSignedUp;
import com.vvk.banque.domain.events.DomainEvent;
import com.vvk.banque.domain.exceptions.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class Customer {

    private final CustomerId id;
    private final String name;
    private final String email;
    private final String hashedPassword;	// impl in app layr to keep domain focused on domain only 

    private final List<DomainEvent> uncommitted = new ArrayList<>();

    // factory for new customer signup
   
 public static Customer signUp(CustomerId id, String name, String email, String hashedPassword) {
        if (name == null || name.trim().isEmpty()) {
            throw new CustomerNameException("Name cannot be null or empty");
        }
        if (email == null || !email.contains("@")) {
            throw new EmailException("Invalid email address");
        }
        if (hashedPassword == null || hashedPassword.trim().isEmpty()) {
            throw new PasswordException("Hashed password cannot be null or empty");
        }

        Customer customer = new Customer(id, name, email, hashedPassword);
        customer.record(new CustomerSignedUp(id, name, email, hashedPassword));
        return customer;
    }

   
 // Rebuild from event stream (for event sourcing)

    public static Customer fromHistry(List<DomainEvent> history) {
        CustomerSignedUp signedUpEvent = history.stream()
                .filter(CustomerSignedUp.class::isInstance)
                .map(CustomerSignedUp.class::cast)
                .findFirst()
                .orElseThrow(() -> new CustomerNotFoundException("history  must contain CustomerSignedUp event"));

        Customer customer = new Customer(
                signedUpEvent.customerId(),
                signedUpEvent.name(),
                signedUpEvent.email(),
                signedUpEvent.hashedPassword()
        );
        history.forEach(customer::apply);
        return customer;
    }

    private Customer(CustomerId id, String name, String email, String hashedPassword) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.hashedPassword = hashedPassword;
    }

    // getters
 
    public CustomerId getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getHashedPassword() { return hashedPassword; } // For verification - in app lyer

    public List<DomainEvent> getUncommittedEvents() {
        return Collections.unmodifiableList(uncommitted);
    }

    public void markEventsAsCommitted() {
        uncommitted.clear();
    }

    private void record(DomainEvent event) {
        apply(event);
        uncommitted.add(event);
    }

    private void apply(DomainEvent event) {
        if (event instanceof CustomerSignedUp) {

            // This method is for rebuilding state from events, but since CustomerSignedUp is the only event, no action needed
        }
        //leaving here to handle other events if added in the future
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Customer)) return false;
        Customer other = (Customer) o;
        return id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}	

