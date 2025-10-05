package com.vvk.banque.domain.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.vvk.banque.domain.ValueObj.CustomerId;

public final class CustomerSignedUp implements DomainEvent {
    private final CustomerId customerId;
    private final String name;
    private final String email;
    private final String hashedPassword;

    // Add @JsonCreator and @JsonProperty annotations
    @JsonCreator
    public CustomerSignedUp(
        @JsonProperty("customerId") CustomerId customerId,
        @JsonProperty("name") String name,
        @JsonProperty("email") String email,
        @JsonProperty("hashedPassword") String hashedPassword) {
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
