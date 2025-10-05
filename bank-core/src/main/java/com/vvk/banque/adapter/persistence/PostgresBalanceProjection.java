package com.vvk.banque.adapter.persistence;

import com.vvk.banque.domain.events.*;
import com.vvk.banque.domain.ValueObj.Money;
import com.vvk.banque.domain.ValueObj.AccountId;
import com.vvk.banque.app.ports.out.AccountQueryPort;
import com.vvk.banque.app.ports.out.AccountEventStorePort; // NEW IMPORT
import com.vvk.banque.domain.AggregatesObj.Account; // NEW IMPORT
import java.sql.*;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.Currency;

public final class PostgresBalanceProjection implements AccountQueryPort {

    private final Connection conn;
    // NEW FIELD: Dependency for loading the full account (from the event store)
    private final AccountEventStorePort eventStore; 

    // MODIFIED CONSTRUCTOR: Now accepts the event store dependency
    public PostgresBalanceProjection(String url, String user, String pass, AccountEventStorePort eventStore) throws SQLException {
        this.conn = DriverManager.getConnection(url, user, pass);
        this.eventStore = eventStore; // Set the new dependency
        conn.createStatement().execute(
            "CREATE TABLE IF NOT EXISTS account_balance(" +
            "acc_no CHAR(5) PRIMARY KEY," +
            "customer VARCHAR(3)," +
            "balance NUMERIC(12,2) NOT NULL," +
            "updated_at TIMESTAMP NOT NULL)"
        );
    }

    public void project(DomainEvent evt) {
        try {
            if (evt instanceof AccountOpened e) {
                upsert(e.getAccountId().getAcc(), e.getOwnerId().getCust(), e.getOpenBal());
            } else if (evt instanceof MoneyDeposited e) {
                add(e.accountId().getAcc(), e.amount());
            } else if (evt instanceof MoneyWithdrawn e) {
                add(e.accountId().getAcc(), e.amount().multiply(BigDecimal.valueOf(-1), java.math.RoundingMode.HALF_EVEN));
            } else if (evt instanceof MoneyTransferSend e) {
                // Already fixed to use fromAccountId() and toAccountId()
                add(e.fromAccountId().getAcc(), e.amount().multiply(BigDecimal.valueOf(-1), java.math.RoundingMode.HALF_EVEN)); 
            } else if (evt instanceof MoneyTransferReceive e) {
                add(e.toAccountId().getAcc(), e.amount());
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error projecting event: " + e.getMessage(), e);
        }
    }
    // ... (upsert and add methods remain unchanged) ...

    private void upsert(int acc, String cust, Money amt) throws SQLException {
        PreparedStatement ps = conn.prepareStatement(
            "INSERT INTO account_balance (acc_no, customer, balance, updated_at) " +
            "VALUES (?, ?, ?, ?) ON CONFLICT (acc_no) DO UPDATE SET balance = ?, updated_at = ?"
        );
        ps.setString(1, String.format("%05d", acc));
        ps.setString(2, cust);
        ps.setBigDecimal(3, amt.getAmt());
        ps.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));
        ps.setBigDecimal(5, amt.getAmt());
        ps.setTimestamp(6, Timestamp.valueOf(LocalDateTime.now()));
        ps.executeUpdate();
    }

    private void add(int acc, Money delta) throws SQLException {
        PreparedStatement ps = conn.prepareStatement(
            "UPDATE account_balance SET balance = balance + ?, updated_at = ? WHERE acc_no = ?"
        );
        ps.setBigDecimal(1, delta.getAmt());
        ps.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
        ps.setString(3, String.format("%05d", acc));
        ps.executeUpdate();
    }

    // REQUIRED IMPLEMENTATION: findBalanceByAccountId (from Step 6)
    @Override
    public Money findBalanceByAccountId(AccountId accountId) {
        try (PreparedStatement ps = conn.prepareStatement(
            "SELECT balance FROM account_balance WHERE acc_no = ?"
        )) {
            ps.setString(1, String.format("%05d", accountId.getAcc()));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Money.of(rs.getBigDecimal("balance"), Currency.getInstance("USD"));
                } else {
                    return Money.of(BigDecimal.ZERO, Currency.getInstance("USD"));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error querying balance for account: " + accountId.toString(), e);
        }
    }

    // FIX: ADDED MISSING loadAccount METHOD (Delegates to the Event Store)
    @Override
    public Account loadAccount(AccountId accountId) {
        return eventStore.loadAccount(accountId);
    }
    
    public String listMasked() throws SQLException {
        ResultSet rs = conn.createStatement().executeQuery(
            "SELECT acc_no, customer, balance, updated_at FROM account_balance ORDER BY acc_no"
        );
        StringBuilder sb = new StringBuilder();
        while (rs.next()) {
            sb.append(rs.getString("acc_no"))
              .append("  ")
              .append(rs.getString("customer"))
              .append("  ")
              .append(rs.getTimestamp("updated_at").toLocalDateTime())
              .append("  ")
              .append(rs.getBigDecimal("balance"))
              .append(" USD\n");
        }
        return sb.isEmpty() ? "no accounts found" : sb.toString();
    }
}
