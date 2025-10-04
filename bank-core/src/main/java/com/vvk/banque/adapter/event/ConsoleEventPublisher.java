package com.vvk.banque.adapter.event;

import com.vvk.banque.app.ports.out.EventPublisherPort;
import com.vvk.banque.domain.events.DomainEvent;

public class ConsoleEventPublisher implements EventPublisherPort {

    @Override
    public void publish(DomainEvent event) {
        // In MVP: print to console (simulates audit log)
        System.out.println("[AUDIT] " + event.getClass().getSimpleName() + ": " + event);
        // Later if time permist:  replace with DynamoDB putItem()
    }
}
