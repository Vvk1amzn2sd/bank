package com.vvk.banque.domain.events;

import com.vvk.banque.domain.ValueObj.*;

public final class CustomerSignedUp implements DomainEvent {
    private final CustomerId customerId;
    private final String name;
    private final String email;
    private final String hashedPassword; 

    public CustomerSignedUp(CustomerId customerId, String name, String email, String hashedPassword) {
        this.customerId = customerId;
        this.name = name;
        this.email = email;
        this.hashedPassword = hashedPassword;
    }

    public CustomerId getCustomerId() { return customerId; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getHashedPassword() { return hashedPassword; }
}
