package com.vvk.banque.adapter.persistence;

import com.vvk.banque.domain.events.*;
import com.vvk.banque.domain.ValueObj.Money;
import com.vvk.banque.domain.ValueObj.AccountId;
import com.vvk.banque.app.ports.out.AccountQueryPort;
import com.vvk.banque.app.ports.out.AccountEventStorePort;
import com.vvk.banque.domain.AggregatesObj.Account;
import java.sql.*;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.Currency;

public final class PostgresBalanceProjection implements AccountQueryPort {

    private final Connection conn;
    private final AccountEventStorePort eventStore; 

    public PostgresBalanceProjection(String url, String user, String pass, AccountEventStorePort eventStore) throws SQLException {
        this.conn = DriverManager.getConnection(url, user, pass);
        this.eventStore = eventStore;
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
            } else if (evt instanceof MoneyTransferInitiated e) { // Handles sender deduction
                add(e.fromId().getAcc(), e.amount().multiply(BigDecimal.valueOf(-1), java.math.RoundingMode.HALF_EVEN));
            } else if (evt instanceof MoneyTransferReceive e) { // Handles receiver addition
                add(e.toAccountId().getAcc(), e.amount());
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error projecting event: " + e.getMessage(), e);
        }
    }

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
