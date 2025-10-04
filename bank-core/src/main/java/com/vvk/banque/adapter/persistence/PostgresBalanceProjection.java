package com.vvk.banque.adapter.persistence;

import com.vvk.banque.domain.events.*;
import com.vvk.banque.domain.ValueObj.Money;
import com.vvk.banque.domain.AggregatesObj.*;
import java.sql.*;
import java.time.LocalDateTime;

public final class PostgresBalanceProjection {

    private final Connection conn;

    public PostgresBalanceProjection(String url, String user, String pass) throws SQLException {
        this.conn = DriverManager.getConnection(url, user, pass);
        conn.createStatement().execute(
            """
            CREATE TABLE IF NOT EXISTS account_balance(
              acc_no CHAR(5) PRIMARY KEY,
              customer VARCHAR(3),
              balance NUMERIC(12,2) NOT NULL,
              updated_at TIMESTAMP NOT NULL
            )
            """
        );
    }

    /* idempotent-> duh, event driven immutable logs thats why was painstakingly built into domain, no ree money here. just  upsert on every event */
    public void project(DomainEvent evt) {
        try {
            switch (evt) {
                case AccountOpened e -> upsert(e.accountId().getAcc(), e.getOwnerId().getCust(), e.getOpenBal());
                case MoneyDeposited e -> add(e.accountId().getAcc(), e.amount());
                case MoneyWithdrawn e -> add(e.accountId().getAcc(), e.amount().multiply(BigDecimal.valueOf(-1)));
                case MoneyTransferSend e -> add(e.fromId().getAcc(), e.amount().multiply(BigDecimal.valueOf(-1)));
                case MoneyTransferReceive e -> add(e.toId().getAcc(), e.amount());
                default -> {}
            }
        } catch (SQLException ex) { throw new RuntimeException(ex); }
    }
/*----checked exception above sqlex -> good here while all other in domain were runtime since its only logical cuz they were all BL violationn, not violation of how computer functions --*/
    private void upsert(int acc, String cust, Money amt) throws SQLException {
        PreparedStatement ps = conn.prepareStatement(
            "INSERT INTO account_balance(acc_no, customer, balance, updated_at) VALUES (?,?,?,?) " +
            "ON CONFLICT(acc_no) DO UPDATE SET balance=?, updated_at=?"
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

    /* used by vvk_ command -> to display no. of acc & histry */
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
        return sb.isEmpty() ? "no accounts\n" : sb.toString();
    }
}
