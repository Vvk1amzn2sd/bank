package com.vvk.banque.cli;

import com.vvk.banque.domain.AggregatesObj.Account;
import com.vvk.banque.domain.AggregatesObj.Customer;
import com.vvk.banque.domain.ValueObj.*;
import com.vvk.banque.adapter.persistence.*;
import com.vvk.banque.domain.events.*;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import com.vvk.banque.app.ports.out.AccountEventStorePort;
import com.vvk.banque.app.ports.out.AccountQueryPort;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.Currency;

public final class BankShell {
    private static final Scanner in = new Scanner(System.in);
    private static final AccountEventStorePort eventStore = chooseEventStore();
    private static final AccountQueryPort queryPort = (AccountQueryPort) eventStore;
    private static final PostgresBalanceProjection readModel = initPostgres();
    private static final Map<String, Customer> customers = new HashMap<>();
    private static final DatabaseSequence seq = new AtomicSequence();
    private static final String SETTLEMENT_PASSWORD = System.getenv().getOrDefault("VVK_SETTLE_PASSWORD", "default_password_if_not_set");

    public static void main(String[] args) { interactive(); }

    public void handle(String line) {
        String[] cmd = line.trim().split("\\s+");
        if (cmd.length == 0) return;
        try {
            switch (cmd[0]) {
                case "help" -> System.out.println(
                    """
                    signup   <3LETTERS> <email> <pwd>          - create customer (e.g.: signup VVK ixnine@amzn.to)
                    open     <5DIGITS> <3LETTERS> <amount> [CUR] - create account (e.g.: open 12345 VVK 100 USD or open 12345 VVK 100 [defaults to USD])
                    deposit  <5DIGITS> <amount> [CUR]            - add money (e.g.: deposit 12345 50 USD)
                    withdraw <5DIGITS> <amount> [CUR]            - take money (e.g.: withdraw 12345 30 USD)
                    transfer <5DIGITS> <5DIGITS> <amount> [CUR]  - move money (e.g.: transfer 12345 54321 20 USD)
                    balance  <5DIGITS>                          - get account balance
                    vvk_list                                    - list all accounts
                    vvk_settle <password>                       - settle pending transfers (only preeya can run, may ask pw from her :-0)
                    exit                                        - quit
                    """
                );
                case "signup" -> {
                    if (!cmd[1].matches("[A-Z]{3}")) {
                        System.out.println("Error: Customer ID must be 3 uppercase letters");
                        return;
                    }
                    CustomerId custId = CustomerId.generate(cmd[1]);
                    Customer c = Customer.signUp(custId, cmd[1], cmd[2], cmd[3]);
                    customers.put(cmd[1], c);
                    System.out.println("Customer created: " + custId);
                }
                case "open" -> {
                    if (!cmd[1].matches("[1-9]\\d{4}")) {
                        System.out.println("Error: Account ID must be 5 digits, first digit 1-9");
                        return;
                    }
                    int accNumber = Integer.parseInt(cmd[1]);
                    AccountId accId = new AccountId(accNumber, UUID.randomUUID());
                    if (!customers.containsKey(cmd[2])) {
                        System.out.println("Error: Customer " + cmd[2] + " not found");
                        return;
                    }
                    CustomerId custId = customers.get(cmd[2]).getCustomerId();

                    BigDecimal amountValue = new BigDecimal(cmd[3]);
                    Currency currency = Currency.getInstance("USD");
                    if (cmd.length >= 5) {
                         try {
                             currency = Currency.getInstance(cmd[4].toUpperCase());
                         } catch (IllegalArgumentException e) {
                              System.out.println("Error: Invalid currency code '" + cmd[4] + "'. Using default USD.");
                         }
                    }
                    Money openingBalance = Money.of(amountValue, currency);
                    Account a = Account.open(accId, custId, openingBalance);
                    saveAndProject(a);
                    System.out.println("Account " + a.getID() + " opened with " + openingBalance);
                }
                case "deposit" -> {
                    Account a = loadAccount(cmd[1]);
                    BigDecimal amountValue = new BigDecimal(cmd[2]);
                    Currency currency = Currency.getInstance("USD");
                    if (cmd.length >= 4) {
                         try {
                             currency = Currency.getInstance(cmd[3].toUpperCase());
                         } catch (IllegalArgumentException e) {
                              System.out.println("Error: Invalid currency code '" + cmd[3] + "'. Using default USD.");
                         }
                    }
                    Money amount = Money.of(amountValue, currency);
                    a.deposit(amount);
                    saveAndProject(a);
                    System.out.println("Deposited " + amount + " to account " + a.getID());
                }
                case "withdraw" -> {
                    Account a = loadAccount(cmd[1]);
                    BigDecimal amountValue = new BigDecimal(cmd[2]);
                    Currency currency = Currency.getInstance("USD");
                    if (cmd.length >= 4) {
                         try {
                             currency = Currency.getInstance(cmd[3].toUpperCase());
                         } catch (IllegalArgumentException e) {
                              System.out.println("Error: Invalid currency code '" + cmd[3] + "'. Using default USD.");
                         }
                    }
                    Money amount = Money.of(amountValue, currency);
                    a.withdraw(amount);
                    saveAndProject(a);
                    System.out.println("Withdrew " + amount + " from account " + a.getID());
                }
                case "transfer" -> {
                    Account from = loadAccount(cmd[1]);
                    Account toAccount = loadAccount(cmd[2]);
                    AccountId toId = toAccount.getID();

                    BigDecimal amountValue = new BigDecimal(cmd[3]);
                    Currency currency = Currency.getInstance("USD");
                    if (cmd.length >= 5) {
                         try {
                             currency = Currency.getInstance(cmd[4].toUpperCase());
                         } catch (IllegalArgumentException e) {
                              System.out.println("Error: Invalid currency code '" + cmd[4] + "'. Using default USD.");
                         }
                    }
                    Money amount = Money.of(amountValue, currency);

                    // Initiate transfer (deduct from sender)
                    from.send(toId, amount);
                    saveAndProject(from);

                    // Immediately process receive (add to receiver)
                    MoneyTransferReceive receiveEvent = new MoneyTransferReceive(toId, from.getID(), amount);
                    eventStore.saveEvent(receiveEvent);
                    readModel.project(receiveEvent);

                    System.out.println("Transfer of " + amount + " completed from " + from.getID() + " to " + toId);
                }
                case "balance" -> {
                    Account account = loadAccount(cmd[1]);
                    Money bal = readModel.findBalanceByAccountId(account.getID());
                    System.out.println(bal);
                }
                case "vvk_list" -> System.out.println(readModel.listMasked());
                case "vvk_settle" -> {
                     if (cmd.length < 2) {
                         System.out.println("Error: vvk_settle requires a password.");
                         return;
                     }
                     String providedPassword = cmd[1];
                     if (!SETTLEMENT_PASSWORD.equals(providedPassword)) {
                         System.out.println("Error: Invalid settlement password.");
                         return;
                     }
                     System.out.println("Settlement process completed (dummy command - transfers are immediate).");
                }
                case "exit" -> System.exit(0);
                default -> System.out.println("Unknown command: " + cmd[0]);
            }
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private static void interactive() {
        System.out.println("Welcome to vvk-bank shell. Type 'help' for commands.");
        String line;
        while ((line = in.nextLine()) != null) {
            System.out.print("$ ");
            new BankShell().handle(line);
        }
    }

    private static Account loadAccount(String accNumberStr) {
        try {
            int accNumber = Integer.parseInt(accNumberStr);
            List<DomainEvent> events = eventStore.loadEventsByNumericAcc(accNumber);
            if (events.isEmpty()) throw new RuntimeException("Account not found: " + accNumberStr);
            AccountId fullAccountId = events.get(0).accountId();
            return eventStore.loadAccount(fullAccountId);
        } catch (NumberFormatException e) {
             throw new RuntimeException("Invalid account number format: " + accNumberStr);
        } catch (Exception e) {
            throw new RuntimeException("Account not found or error loading: " + accNumberStr, e);
        }
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
        String cosmosUri = System.getenv("COSMOS_URI");
        String cosmosKey = System.getenv("COSMOS_KEY");
        if (cosmosUri == null || cosmosKey == null) {
            throw new IllegalStateException("COSMOS_URI and COSMOS_KEY environment variables must be set to connect to Azure Cosmos DB event store.");
        }
        return new CosmosEventStore(cosmosUri, cosmosKey, "bankdb", "bank-events");
    }

    private static PostgresBalanceProjection initPostgres() {
        String url  = System.getenv("POSTGRES_URL");
        String user = System.getenv("POSTGRES_USER");
        String pass = System.getenv("POSTGRES_PASS");
        if (url == null || user == null || pass == null)
            throw new IllegalStateException("Postgres env vars missing. Set POSTGRES_URL, POSTGRES_USER, and POSTGRES_PASS.");
        try { return new PostgresBalanceProjection(url, user, pass, eventStore); }
        catch (Exception e) { throw new RuntimeException(e); }
    }

    private static final class AtomicSequence implements DatabaseSequence {
        private final AtomicInteger counter = new AtomicInteger(10_000);
        @Override public int nextAcc() { return counter.getAndIncrement(); }
    }
}
