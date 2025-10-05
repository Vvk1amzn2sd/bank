package com.vvk.banque.adapter.persistence;

import com.azure.cosmos.CosmosClient;
import com.azure.cosmos.CosmosContainer;
import com.azure.cosmos.CosmosClientBuilder;
import com.azure.cosmos.models.CosmosQueryRequestOptions;
import com.azure.cosmos.models.PartitionKey;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    private final ObjectMapper mapper = new ObjectMapper();

    public CosmosEventStore(String uri, String key, String dbName, String containerName) {
        CosmosClient client = new CosmosClientBuilder()
                .endpoint(uri)
                .key(key)
                .preferredRegions(List.of("West India"))
                .buildClient();
        this.container = client.getDatabase(dbName).getContainer(containerName);
    }

    @Override
    public void saveEvent(DomainEvent event) {
        Map<String, Object> doc = Map.of(
                "id", UUID.randomUUID().toString(),
                "accountId", event.accountId().toString(),
                "eventType", event.getClass().getSimpleName(),
                "payload", mapper.valueToTree(event),
                "timestamp", Instant.now().toString()
        );
        container.createItem(doc, new PartitionKey(event.accountId().toString()), null);
    }

    @Override
    public List<DomainEvent> loadEvents(AccountId accountId) {
        String query = "SELECT * FROM c WHERE c.accountId = @aid ORDER BY c.timestamp ASC";
        List<JsonNode> docs = container.queryItems(
                query,
                new CosmosQueryRequestOptions(),
                JsonNode.class
        ).stream().collect(Collectors.toList());
        if (docs.isEmpty()) throw new RuntimeException("Account not found: " + accountId);
        return docs.stream().map(this::toDomainEvent).collect(Collectors.toList());
    }

    @Override
    public Account loadAccount(AccountId accountId) {
        return Account.fromHistry(accountId, loadEvents(accountId));
    }

    @Override
    public Money findBalanceByAccountId(AccountId accountId) {
        return loadAccount(accountId).getBalance();
    }

    private DomainEvent toDomainEvent(JsonNode node) {
        String type = node.get("eventType").asText();
        JsonNode payload = node.get("payload");
        return switch (type) {
            case "AccountOpened"      -> mapper.convertValue(payload, AccountOpened.class);
            case "MoneyDeposited"     -> mapper.convertValue(payload, MoneyDeposited.class);
            case "MoneyWithdrawn"     -> mapper.convertValue(payload, MoneyWithdrawn.class);
            case "MoneyTransferSend"  -> mapper.convertValue(payload, MoneyTransferSend.class);
            case "MoneyTransferReceive" -> mapper.convertValue(payload, MoneyTransferReceive.class);
            default -> throw new IllegalArgumentException("Unknown event type: " + type);
        };
    }
}

