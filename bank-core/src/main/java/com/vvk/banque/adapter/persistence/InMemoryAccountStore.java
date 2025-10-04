package com.vvk.banque.adapter.persistence;

import com.vvk.banque.app.ports.out.AccountEventStorePort;
import com.vvk.banque.app.ports.out.AccountQueryPort;
import com.vvk.banque.domain.AggregatesObj.Account;
import com.vvk.banque.domain.ValueObj.AccountId;
import com.vvk.banque.domain.ValueObj.Money;
import com.vvk.banque.domain.events.DomainEvent;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

// Local fallback store for when no cloud provider (AWS/Azure) is configured.
// Stores events in memory, which is volatile.
public final class InMemoryAccountStore implements AccountEventStorePort, AccountQueryPort {

    // Key: AccountId (String representation), Value: List of DomainEvent
    private final Map<String, List<DomainEvent>> eventStreams = new ConcurrentHashMap<>();

    @Override
    public void saveEvent(DomainEvent event) {
        // Ensure the list for the account exists, then add the new event.
        eventStreams.computeIfAbsent(
                event.accountId().toString(), 
                k -> new CopyOnWriteArrayList<>()
        ).add(event);
    }

    @Override
    public List<DomainEvent> loadEvents(AccountId accountId) {
        String key = accountId.toString();
        // Return a copy of the list to prevent external modification, or an empty list if not found.
        return eventStreams.getOrDefault(key, Collections.emptyList()).stream().collect(Collectors.toList());
    }

    @Override
    public Account loadAccount(AccountId accountId) {
        List<DomainEvent> history = loadEvents(accountId);
        if (history.isEmpty()) {
            throw new RuntimeException("Account not found in in-memory store: " + accountId);
        }
        return Account.fromHistry(accountId, history);
    }

    // Since this is an event store, finding balance requires rebuilding the account state.
    // In a real CQRS application, this would query a dedicated read model.
    @Override
    public Money findBalanceByAccountId(AccountId accountId) {
        // This is inefficient but necessary for a simple in-memory store acting as both event store and query port.
        return loadAccount(accountId).getBalance();
    }
}

