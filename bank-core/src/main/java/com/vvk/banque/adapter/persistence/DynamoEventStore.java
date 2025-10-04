package com.vvk.banque.adapter.persistence;

import com.vvk.banque.app.ports.out.AccountEventStorePort;
import com.vvk.banque.app.ports.out.AccountQueryPort;
import com.vvk.banque.domain.AggregatesObj.Account;
import com.vvk.banque.domain.ValueObj.AccountId;
import com.vvk.banque.domain.events.*;
import com.vvk.banque.domain.ValueObj.Money;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;
import software.amazon.awssdk.core.SdkBytes;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

public final class DynamoEventStore implements AccountEventStorePort, AccountQueryPort {

    private final DynamoDbClient client;
    private final String tableName;
    private final ObjectMapper mapper = new ObjectMapper();

    public DynamoEventStore(DynamoDbClient client, String tableName) {
        this.client = client;
        this.tableName = tableName;
    }

    @Override
    public void saveEvent(DomainEvent event) {
        Map<String, AttributeValue> item = Map.of(
                "accountId", AttributeValue.builder().s(event.accountId().toString()).build(),
                "eventId",   AttributeValue.builder().s(UUID.randomUUID().toString()).build(),
                "eventType", AttributeValue.builder().s(event.getClass().getSimpleName()).build(),
                "payload",   AttributeValue.builder().b(SdkBytes.fromUtf8String(toJson(event))).build(),
                "timestamp", AttributeValue.builder().s(Instant.now().toString()).build()
        );
        client.putItem(PutItemRequest.builder().tableName(tableName).item(item).build());
    }

    @Override
    public List<DomainEvent> loadEvents(AccountId accountId) {
        QueryRequest req = QueryRequest.builder()
                .tableName(tableName)
                .keyConditionExpression("accountId = :aid")
                .expressionAttributeValues(Map.of(":aid", AttributeValue.builder().s(accountId.toString()).build()))
                .build();
        List<Map<String, AttributeValue>> items = client.query(req).items();
        if (items.isEmpty()) throw new RuntimeException("Account not found: " + accountId);
        return items.stream().map(this::toDomainEvent).collect(Collectors.toList());
    }

    @Override
    public Account loadAccount(AccountId accountId) {
        return Account.fromHistry(accountId, loadEvents(accountId));
    }

    @Override
    public Money findBalanceByAccountId(AccountId accountId) {
        return loadAccount(accountId).getBalance();
    }

    /* ---------- helpers ---------- */
    private String toJson(Object o) {
        try { return mapper.writeValueAsString(o); }
        catch (Exception e) { throw new RuntimeException(e); }
    }

    private DomainEvent toDomainEvent(Map<String, AttributeValue> item) {
        String type = item.get("eventType").s();
        JsonNode node;
        try { node = mapper.readTree(item.get("payload").b().asUtf8String()); }
        catch (Exception e) { throw new RuntimeException(e); }
        return switch (type) {
            case "AccountOpened"      -> mapper.convertValue(node, AccountOpened.class);
            case "MoneyDeposited"     -> mapper.convertValue(node, MoneyDeposited.class);
            case "MoneyWithdrawn"     -> mapper.convertValue(node, MoneyWithdrawn.class);
            case "MoneyTransferSend"  -> mapper.convertValue(node, MoneyTransferSend.class);
            case "MoneyTransferReceive" -> mapper.convertValue(node, MoneyTransferReceive.class);
            default -> throw new IllegalArgumentException("Unknown event type: " + type);
        };
    }
}
