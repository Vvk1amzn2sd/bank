package com.vvk.banque.adapter.persistence;
import com.azure.cosmos.CosmosClient;
import com.azure.cosmos.CosmosContainer;
import com.azure.cosmos.CosmosClientBuilder;
import com.azure.cosmos.models.CosmosQueryRequestOptions; 
import com.azure.cosmos.models.PartitionKey;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import com.fasterxml.jackson.databind.DeserializationFeature;

import com.vvk.banque.app.ports.out.AccountEventStorePort;
import com.vvk.banque.app.ports.out.AccountQueryPort;
import com.vvk.banque.domain.AggregatesObj.Account;
import com.vvk.banque.domain.ValueObj.AccountId;
import com.vvk.banque.domain.events.*;
import com.vvk.banque.domain.ValueObj.Money;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

public final class CosmosEventStore implements AccountEventStorePort, AccountQueryPort {
    private final CosmosContainer container;
    private final ObjectMapper mapper = new ObjectMapper(); // Keep the instance

    public CosmosEventStore(String uri, String key, String dbName, String containerName) {
        CosmosClient client = new CosmosClientBuilder()
                .endpoint(uri)
                .key(key)
                .preferredRegions(List.of("West India"))
                .buildClient();
        this.container = client.getDatabase(dbName).getContainer(containerName);

        // Configure the ObjectMapper instance for Records and Parameter Names
        this.mapper.registerModule(new ParameterNamesModule());
        // Configure the ObjectMapper to ignore unknown properties during deserialization
        this.mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @Override
    public void saveEvent(DomainEvent event) {
        Map<String, Object> doc = Map.of(
                "id", UUID.randomUUID().toString(),
                "accountId", event.accountId().toString(), // <-- This stores "XXXXX-UUID_STRING"
                "eventType", event.getClass().getSimpleName(),
                "payload", mapper.valueToTree(event), // Uses the configured mapper
                "timestamp", Instant.now().toString()
        );
        // Use the AccountId string as the partition key
        container.createItem(doc, new PartitionKey(event.accountId().toString()), null);
    }

    @Override
    public List<DomainEvent> loadEvents(AccountId accountId) {
        // Use string concatenation for exact match query - NO CosmosSqlParameter
        String accountIdStr = accountId.toString();
        String query = "SELECT * FROM c WHERE c.accountId = '" + accountIdStr + "' ORDER BY c.timestamp ASC";

        List<JsonNode> docs = container.queryItems(
                query, // The query string now contains the ID directly
                new CosmosQueryRequestOptions(), // Use default options
                JsonNode.class
        ).stream().collect(Collectors.toList());

        if (docs.isEmpty()) throw new RuntimeException("Account not found: " + accountId);
        return docs.stream().map(this::toDomainEvent).collect(Collectors.toList()); // Uses the configured mapper
    }

    @Override
    public Account loadAccount(AccountId accountId) {
        return Account.fromHistry(accountId, loadEvents(accountId));
    }

    @Override
    public List<DomainEvent> loadEventsByNumericAcc(int accNumber) {
        // Construct the prefix string to match
        String prefix = String.format("%05d", accNumber) + "-"; // e.g., "44444-"
        System.out.println("DEBUG: Querying CosmosDB for prefix: " + prefix); // Debug print

        // Use string concatenation for STARTSWITH query - NO CosmosSqlParameter
        // IMPORTANT: The prefix must be enclosed in single quotes within the query string for CosmosDB SQL API
        String query = "SELECT * FROM c WHERE STARTSWITH(c.accountId, '" + prefix + "') ORDER BY c.timestamp ASC";

        List<JsonNode> docs = container.queryItems(
                query, // The query string now contains the prefix directly
                new CosmosQueryRequestOptions(), // Use default options
                JsonNode.class
        ).stream().collect(Collectors.toList());

        System.out.println("DEBUG: Found " + docs.size() + " events for prefix: " + prefix); // Debug print
        return docs.stream().map(this::toDomainEvent).collect(Collectors.toList()); // Uses the configured mapper
    }

    @Override
    public Money findBalanceByAccountId(AccountId accountId) {
        return loadAccount(accountId).getBalance();
    }

    private DomainEvent toDomainEvent(JsonNode node) {
        String type = node.get("eventType").asText();
        JsonNode payload = node.get("payload");
        // Use the configured ObjectMapper instance for deserialization
        return switch (type) {
            case "AccountOpened"      -> mapper.convertValue(payload, AccountOpened.class); // Uses mapper
            case "MoneyDeposited"     -> mapper.convertValue(payload, MoneyDeposited.class); // Uses mapper
            case "MoneyWithdrawn"     -> mapper.convertValue(payload, MoneyWithdrawn.class); // Uses mapper
            case "MoneyTransferInitiated" -> mapper.convertValue(payload, MoneyTransferInitiated.class); // Uses mapper
            // Keep old ones for compatibility if needed during transition
            case "MoneyTransferSend"  -> mapper.convertValue(payload, MoneyTransferSend.class); // Uses mapper
            case "MoneyTransferReceive" -> mapper.convertValue(payload, MoneyTransferReceive.class); // Uses mapper
            default -> throw new IllegalArgumentException("Unknown event type: " + type);
        };
    }
}
