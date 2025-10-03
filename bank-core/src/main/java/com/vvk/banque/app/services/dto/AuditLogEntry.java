package com.vvk.banque.app.services.dto;

import java.time.Instant;
import com.vvk.banque.domain.ValueObj.AccountId;

public final class AuditLogEntry {

    private final AccountId accountId;
    private final String action;
    private final Instant occurredAt;
    private final String metadata;

    public AuditLogEntry(AccountId accountId, String action, Instant occurredAt, String metadata) {
        this.accountId = accountId;
        this.action = action;
        this.occurredAt = occurredAt;
        this.metadata = metadata;
    }

    public AccountId getAccountId() {
        return accountId;
    }

    public String getAction() {
        return action;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public String getMetadata() {
        return metadata;
    }
}
