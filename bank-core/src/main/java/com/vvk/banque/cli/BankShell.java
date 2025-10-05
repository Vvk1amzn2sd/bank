package com.vvk.banque.cli;

import com.vvk.banque.domain.AggregatesObj.Account;
import com.vvk.banque.domain.AggregatesObj.Customer;
import com.vvk.banque.domain.ValueObj.*;
import com.vvk.banque.adapter.persistence.*;
import com.vvk.banque.domain.events.DomainEvent;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import com.vvk.banque.app.ports.out.AccountEventStorePort;
import com.vvk.banque.app.ports.out.AccountQueryPort;

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
                    withdraw <acc> <usd>           - take money
                    transfer <from> <to> <usd>     - move money
                    balance  <acc>                 - get account balance (read model)
                    vvk_list                       - list all accounts (read model)
                    exit                           - quit
                    """
                );
                case "signup" -> {
                    CustomerId custId = new CustomerId(cmd[1]);
                    Customer c = Customer.signUp(
                        custId,
                        cmd[1], // customer name as ID for simplicity
                        cmd[2],
                        cmd[3]  // Hashed password
                    );
                    customers.put(cmd[1], c);
                    System.out.println("Customer created: " + custId);
                }
                case "open" -> {
                    AccountId accId = new AccountId(cmd[1]);
                    CustomerId custId = new CustomerId(cmd[2]);
                    Money openingBalance = money(cmd[3]);

                    if (!customers.containsKey(cmd[2])) {
                        System.out.println("Error: Customer " + custId + " not found.");
                        return;
                    }
                    
                    Account a = Account.open(accId, custId, openingBalance);
                    saveAndProject(a);
                    cache.put(cmd[1], a);
                    System.out.printf("Account %s opened with %s%n", accId, openingBalance);
                }
                case "deposit" -> {
                    Account a = loadAccount(cmd[1]);
                    a.deposit(money(cmd[2]));
                    saveAndProject(a);
                    System.out.printf("Deposited %s to %s%n", money(cmd[2]), a.getAID());
                }
                case "withdraw" -> {
                    Account a = loadAccount(cmd[1]);
                    a.withdraw(money(cmd[2]));
                    saveAndProject(a);
                    System.out.printf("Withdrew %s from %s%n", money(cmd[2]), a.getAID());
                }
                case "transfer" -> {
                    AccountId senderId = new AccountId(cmd[1]);
                    AccountId beneficiaryId = new AccountId(cmd[2]);
                    Money amount = money(cmd[3]);
                    
                    // 1. Load, Command (Debit), Save Sender (Transaction A)
                    // If the sender's save fails (e.g., OCC failure), the system rolls back.
                    Account sender = loadAccount(senderId);
                    sender.send(beneficiaryId, amount); 
                    saveAndProject(sender);
                    
                    // 2. Load, Command (Credit), Save Beneficiary (Transaction B)
                    // In a real system, this would be an eventual consistency step (Saga/Process Manager).
                    // Here, we execute it immediately to complete the synchronous shell command.
                    Account beneficiary = loadAccount(beneficiaryId);
                    beneficiary.receive(senderId, amount); 
                    saveAndProject(beneficiary);

                    System.out.printf("Transfer of %s from %s to %s completed.%n", amount, senderId, beneficiaryId);
                }
                case "balance" -> {
                    Money bal = readModel.findBalanceByAccountId(new AccountId(cmd[1]));
                    System.out.printf("Balance for %s is %s%n", cmd[1], bal);
                }
                case "vvk_list" -> System.out.print(readModel.listMasked());
                case "exit" -> System.out.println("Goodbye.");
                default -> System.out.println("Unknown command: " + cmd[0]);
            }
        } catch (Exception e) {
            // Using e.getMessage() to print concise errors to the CLI/Web terminal
            System.out.println("Error: " + e.getMessage());
        }
    }

    private static void interactive() {
        System.out.println("Welcome to vvk-bank shell. Type 'help' for commands.");
        String line;
        while (true) {
            System.out.print("$ ");
            if (!(in.hasNextLine() && !(line = in.nextLine()).equals("exit"))) break;
            new BankShell().handle(line);
        }
    }

    /* ---------- helpers ---------- */

    private Account loadAccount(String accIdStr) {
        // Simple cache for CLI, in real app would rely fully on event store
        Account a = cache.get(accIdStr);
        if (a == null) {
             a = eventStore.loadAccount(new AccountId(accIdStr));
             cache.put(accIdStr, a);
        }
        return a;
    }

    private Money money(String amt) {
        return new Money(new BigDecimal(amt));
    }

    private void saveAndProject(Account a) {
        // The persistence of uncommitted events to the event store
        // and projection to the read model
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

