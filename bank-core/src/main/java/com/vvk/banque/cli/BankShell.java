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
    private static final DatabaseSequence seq = new AtomicSequence();

    public static void main(String[] args) { interactive(); }

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
                    CustomerId custId = CustomerId.generate(cmd[1]);
                    Customer c = Customer.signUp(custId, cmd[1], cmd[2], cmd[3]);
                    customers.put(cmd[1], c);
                    System.out.println("Customer created: " + custId);
                }
                case "open" -> {
                    AccountId accId = AccountId.fromString(cmd[1]);
                    CustomerId custId = CustomerId.generate(cmd[2]);
                    Money openingBalance = new Money(new BigDecimal(cmd[3]), Currency.getInstance("USD"));
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
                    a.deposit(new Money(new BigDecimal(cmd[2]), Currency.getInstance("USD")));
                    saveAndProject(a);
                    System.out.printf("Deposited %s to %s%n", cmd[2], a.getAID());
                }
                case "withdraw" -> {
                    Account a = loadAccount(cmd[1]);
                    a.withdraw(new Money(new BigDecimal(cmd[2]), Currency.getInstance("USD")));
                    saveAndProject(a);
                    System.out.printf("Withdrew %s from %s%n", cmd[2], a.getAID());
                }
                case "transfer" -> {
                    Account from = loadAccount(cmd[1]);
                    Account to   = loadAccount(cmd[2]);
                    Money amount = new Money(new BigDecimal(cmd[3]), Currency.getInstance("USD"));
                    from.transferTo(to, amount);
                    saveAndProject(from); saveAndProject(to);
                    System.out.printf("Transferred %s from %s to %s%n", amount, cmd[1], cmd[2]);
                }
                case "balance" -> {
                    Money bal = readModel.findBalanceByAccountId(AccountId.fromString(cmd[1]));
                    System.out.printf("Balance for %s is %s%n", cmd[1], bal);
                }
                case "vvk_list" -> System.out.print(readModel.listMasked());
                case "exit" -> System.out.println("Goodbye.");
                default -> System.out.println("Unknown command: " + cmd[0]);
            }
        } catch (Exception e) {
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

    private static Account loadAccount(String accIdStr) {
        Account a = cache.get(accIdStr);
        if (a == null) {
            a = queryPort.loadAccount(AccountId.fromString(accIdStr));
            cache.put(accIdStr, a);
        }
        return a;
    }

    private static void saveAndProject(Account a) {
        a.getUncommittedEvents().forEach(evt -> {
            eventStore.saveEvent(evt);
            readModel.project(evt);
        });
        a.markEventsAsCommitted();
    }

    private static AccountEventStorePort chooseEventStore() {
        String cloud = System.getenv("CLOUD");
        if ("aws".equals(cloud)) return new DynamoEventStore(DynamoDbClient.create(), "bank-events");
        if ("azure".equals(cloud)) return new CosmosEventStore(
                System.getenv("COSMOS_URI"),
                System.getenv("COSMOS_KEY"),
                "bankdb",
                "bank-events");
        return new InMemoryAccountStore();
    }

    private static PostgresBalanceProjection initPostgres() {
        String url  = System.getenv("POSTGRES_URL");
        String user = System.getenv("POSTGRES_USER");
        String pass = System.getenv("POSTGRES_PASS");
        if (url == null || user == null || pass == null)
            throw new IllegalStateException("Postgres env vars missing");
        try { return new PostgresBalanceProjection(url, user, pass, eventStore); }
        catch (Exception e) { throw new RuntimeException(e); }
    }

    private static final class AtomicSequence implements DatabaseSequence {
        private final AtomicInteger counter = new AtomicInteger(10_000);
        @Override public int nextAcc() { return counter.getAndIncrement(); }
    }
}
