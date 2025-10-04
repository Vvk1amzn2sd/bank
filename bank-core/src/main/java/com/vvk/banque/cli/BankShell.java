package com.vvk.banque.cli;

import com.vvk.banque.domain.AggregatesObj.Account;
import com.vvk.banque.domain.AggregatesObj.Customer;
import com.vvk.banque.domain.ValueObj.*;
import com.vvk.banque.adapter.persistence.*;
import com.vvk.banque.domain.events.DomainEvent;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public final class BankShell {

    private static final Scanner in = new Scanner(System.in);
    private static final AccountEventStorePort eventStore = chooseEventStore();
    private static final AccountQueryPort queryPort = (AccountQueryPort) eventStore;
    private static final PostgresBalanceProjection readModel = initPostgres();
    private static final Map<String, Account>  cache = new HashMap<>();
    private static final Map<String, Customer> customers = new HashMap<>();
    private static final DatabaseSequence seq = new AtomicSequence();   // 5-digit generator

    public static void main(String[] args) { interactive(); }

    /* ==========  HTTP bridge entry  ========== */
    public void handle(String line) {
        String[] cmd = line.trim().split("\\s+");
        if (cmd.length == 0) return;
        try {
            switch (cmd[0]) {
                case "help" -> System.out.println(
                    """
                    signup   <cust> <email> <pwd>  - create customer
                    open     <acc> <cust> <usd>    - create account (customer must exist)
                    deposit  <acc> <usd>           - add money
                    withdraw <acc> <usd>           - remove money
                    transfer <from> <to> <usd>     - move money
                    balance  <acc>                 - show balance
                    history  <acc>                 - event count
                    vvk_                           - list accounts (masked)
                    exit                           - quit
                    """
                );
                case "signup" -> signup(cmd);
                case "vvk_"   -> listAccounts();
                case "exit"   -> System.out.println("bye");
                default       -> dispatch(cmd);
            }
        } catch (Exception ex) { System.out.println("err: " + ex.getMessage()); }
    }

    private static void interactive() {
        while (true) {
            String[] cmd = in.nextLine().trim().split("\\s+");
            if (cmd.length == 0) continue;
            if ("exit".equals(cmd[0])) return;
            try { dispatch(cmd); } catch (Exception ex) { System.out.println("err: " + ex.getMessage()); }
        }
    }

    private static void dispatch(String[] cmd) {
        switch (cmd[0]) {
            case "open" -> open(cmd);
            case "deposit" -> deposit(cmd);
            case "withdraw" -> withdraw(cmd);
            case "transfer" -> transfer(cmd);
            case "balance" -> balance(cmd);
            case "history" -> history(cmd);
            default -> System.out.println("unknown");
        }
    }

    /* ========== commands ========== */
    private static void signup(String[] c) {
        if (c.length != 4) throw new RuntimeException("usage: signup <cust> <email> <pwd>");
        CustomerId cid = CustomerId.generate(c[1]);
        Customer cst = Customer.signUp(cid, c[1], c[2], c[3]);
        customers.put(cid.getCust(), cst);
        cst.getUncommittedEvents().forEach(eventStore::saveEvent);
        cst.markEventsAsCommitted();
        System.out.println("customer " + cid.getCust());
    }

    private static void open(String[] c) {
        if (c.length != 4) throw new RuntimeException("usage: open <acc> <cust> <usd>");
        Customer cust = customers.values().stream()
                                 .filter(cs -> cs.getCust().equals(c[2]))
                                 .findFirst()
                                 .orElseThrow(() -> new RuntimeException("customer not found"));
        int accNum = seq.nextAcc();
        AccountId aid = new AccountId(accNum, UUID.randomUUID());
        Money m = new Money(new BigDecimal(c[3]), Currency.getInstance("USD"));
        Account acc = Account.open(aid, cust.getId(), m);
        cache.put(aid.toString(), acc);
        acc.getUncommittedEvents().forEach(evt -> {
            eventStore.saveEvent(evt);
            readModel.project(evt);
        });
        acc.markEventsAsCommitted();
        System.out.println("opened " + aid);
    }

    private static void deposit(String[] c)  {
        Account a = load(c[1]);
        Money m = new Money(new BigDecimal(c[2]), Currency.getInstance("USD"));
        a.deposit(m);
        commit(a);
        System.out.println("deposited");
    }
    private static void withdraw(String[] c) {
        Account a = load(c[1]);
        Money m = new Money(new BigDecimal(c[2]), Currency.getInstance("USD"));
        a.withdraw(m);
        commit(a);
        System.out.println("withdrawn");
    }
    private static void transfer(String[] c) {
        Account from = load(c[1]);
        Account to   = load(c[2]);
        Money m = new Money(new BigDecimal(c[3]), Currency.getInstance("USD"));
        from.transferTo(to, m);
        commit(from); commit(to);
        System.out.println("transferred");
    }
    private static void balance(String[] c)  {
        Account a = load(c[1]);
        System.out.println("balance " + a.getBalance().getAmt());
    }
    private static void history(String[] c)  {
        AccountId aid = AccountId.fromString(c[1]);
        System.out.println("events " + eventStore.loadEvents(aid).size());
    }
    private static void listAccounts() {
        try { System.out.print(readModel.listMasked()); }
        catch (Exception e) { System.out.println("err: " + e.getMessage()); }
    }

    /* ---------- helpers ---------- */
    private static Account load(String raw) {
        AccountId aid = AccountId.fromString(raw);
        return cache.computeIfAbsent(raw, k -> queryPort.loadAccount(aid));
    }
    private static void commit(Account a) {
        a.getUncommittedEvents().forEach(evt -> {
            eventStore.saveEvent(evt);
            readModel.project(evt);
        });
        a.markEventsAsCommitted();
    }

    /* ---------- store selector ---------- */
    private static AccountEventStorePort chooseEventStore() {
        String cloud = System.getenv("CLOUD");
        if ("aws".equals(cloud)) return new DynamoEventStore(DynamoDbClient.create(), "bank-events");
        if ("azure".equals(cloud)) return new CosmosEventStore(
                System.getenv("COSMOS_URI"),
                System.getenv("COSMOS_KEY"),
                "bankdb",
                "bank-events");
        return new InMemoryAccountStore(); // local fallback
    }
    private static PostgresBalanceProjection initPostgres() {
        String url  = System.getenv("POSTGRES_URL");
        String user = System.getenv("POSTGRES_USER");
        String pass = System.getenv("POSTGRES_PASS");
        if (url == null || user == null || pass == null)
            throw new IllegalStateException("Postgres env vars missing");
        try { return new PostgresBalanceProjection(url, user, pass); }
        catch (Exception e) { throw new RuntimeException(e); }
    }

    /* ---------- 5-digit sequence ---------- */
    private static final class AtomicSequence implements DatabaseSequence {
        private final AtomicInteger counter = new AtomicInteger(10_000);
        @Override public int nextAcc() { return counter.getAndIncrement(); }
    }
}
