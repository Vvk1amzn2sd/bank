package com.vvk.banque.cli;

import com.vvk.banque.domain.AggregatesObj.Account;
import com.vvk.banque.domain.AggregatesObj.Customer;
import com.vvk.banque.domain.ValueObj.AccountId;
import com.vvk.banque.domain.ValueObj.CustomerId;
import com.vvk.banque.domain.ValueObj.Money;
import com.vvk.banque.domain.events.DomainEvent;
import com.vvk.banque.adapter.persistence.DatabaseSequence;
import com.vvk.banque.adapter.persistence.DynamoEventStore;
import com.vvk.banque.adapter.persistence.CosmosEventStore;
import com.vvk.banque.adapter.persistence.PostgresBalanceProjection;
import com.vvk.banque.adapter.persistence.InMemoryAccountStore;
import com.vvk.banque.app.ports.out.AccountEventStorePort;
import com.vvk.banque.app.ports.out.AccountQueryPort;

import java.io.PrintStream;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient; 

// This class is the application service layer, handling command parsing and domain coordination.
public final class BankShell {

    // Standard CLI input scanner for interactive mode
    private static final Scanner in = new Scanner(System.in);

    // Event Store (Write Model) - Chooses based on CLOUD environment variable
    private static final AccountEventStorePort eventStore = chooseEventStore();
    
    // Query Port (Read Model/View) - For real-time balance checks (can be the event store itself)
    private static final AccountQueryPort queryPort = (AccountQueryPort) eventStore;
    
    // Postgres Projection (Dedicated Read Model/CQRS view) - Used for denormalized views like listMasked()
    private static final PostgresBalanceProjection readModel = initPostgres();
    
    // Caching/In-Memory Stores (Non-Event Sourced data / convenience cache)
    private static final Map<String, Account>  cache = new HashMap<>();     // Cache of loaded/modified accounts
    private static final Map<String, Customer> customers = new HashMap<>(); // Simple Customer register (for demo)
    
    // Sequence generator for unique 5-digit account IDs
    private static final DatabaseSequence seq = new AtomicSequence(); 

    // Public entry point for interactive console mode
    public static void main(String[] args) { interactive(); }

    /* ==========  HTTP bridge entry (Used by BankShellDemo)  ========== */
    // Allows the shell logic to be executed from a web request, routing output to a PrintStream.
    public void handle(String line) {
        // Simple command parsing: splits line by whitespace
        String[] cmd = line.trim().split("\\s+");
        if (cmd.length == 0 || cmd[0].isEmpty()) return;
        
        try {
            switch (cmd[0]) {
                case "help" -> System.out.println(
                    """
                    signup   <cust> <email> <pwd>  - create customer (e.g. signup vv 'vvk@mail' 123)
                    open     <cust> <usd>          - create new account for customer
                    deposit  <acc> <usd>           - deposit money to an account
                    withdraw <acc> <usd>           - withdraw money from an account
                    transfer <from> <to> <usd>     - transfer money between accounts
                    balance  <acc>                 - show account balance (from query port)
                    vvk_                         - list all accounts (from postgres read model)
                    quit/exit/bye                - exit shell
                    """
                );
                case "signup"   -> commandSignup(cmd);
                case "open"     -> commandOpen(cmd);
                case "deposit"  -> commandDeposit(cmd);
                case "withdraw" -> commandWithdraw(cmd);
                case "transfer" -> commandTransfer(cmd);
                case "balance"  -> commandBalance(cmd);
                case "vvk_"     -> commandVvk(cmd);
                case "quit", "exit", "bye" -> System.out.println("bye."); System.exit(0);
                default -> System.err.println("unknown command: " + cmd[0] + ". Type 'help' for commands.");
            }
        } catch (Exception e) {
            // Log the user-facing message for domain exceptions (e.g., InsufficientBalanceException).
            // For system errors (like DB connection failure), the full stack trace will be printed for debugging.
            System.err.println("ERROR: " + e.getMessage());
        }
    }

    /* ==========  Interactive CLI Mode (Main function)  ========== */
    private static void interactive() {
        System.out.println("vvk-bank event-sourced shell. Type 'help' for commands.");
        System.out.print("$ ");
        while (in.hasNextLine()) {
            new BankShell().handle(in.nextLine());
            System.out.print("$ ");
        }
    }

    /* ==========  Command Implementations  ========== */

    // Implementation for signup command
    private static void commandSignup(String[] cmd) {
        if (cmd.length != 4) throw new IllegalArgumentException("Usage: signup <cust> <email> <pwd>");
        String custId = cmd[1];
        String email = cmd[2];
        String password = cmd[3]; 

        if (customers.containsKey(custId)) throw new IllegalArgumentException("Customer already exists: " + custId);

        Customer customer = Customer.signUp(
            new CustomerId(custId), 
            custId, 
            email, 
            password
        );

        customers.put(custId, customer);
        System.out.println("Customer signed up: " + customer.getId());
    }

    // Implementation for open command
    private static void commandOpen(String[] cmd) {
        if (cmd.length != 3) throw new IllegalArgumentException("Usage: open <cust> <usd>");
        String custId = cmd[1];
        Money openBal = Money.of(new BigDecimal(cmd[2]));

        if (!customers.containsKey(custId)) throw new IllegalArgumentException("Customer not found: " + custId);

        // 1. Generate ID and create the aggregate
        int accNo = seq.nextAcc();
        AccountId accId = new AccountId(accNo);
        CustomerId ownerId = new CustomerId(custId);

        Account account = Account.open(accId, ownerId, openBal);

        // 2. Save event(s) and project to read model
        saveAndProject(account);

        cache.put(accId.toString(), account);
        System.out.println("Account opened: " + accId + " with balance " + openBal);
    }

    // Implementation for deposit command
    private static void commandDeposit(String[] cmd) {
        if (cmd.length != 3) throw new IllegalArgumentException("Usage: deposit <acc> <usd>");
        AccountId accId = new AccountId(Integer.parseInt(cmd[1]));
        Money amount = Money.of(new BigDecimal(cmd[2]));

        Account account = loadAccount(accId);
        account.deposit(amount);
        saveAndProject(account);

        System.out.println("Deposited " + amount + " to account " + accId);
    }

    // Implementation for withdraw command
    private static void commandWithdraw(String[] cmd) {
        if (cmd.length != 3) throw new IllegalArgumentException("Usage: withdraw <acc> <usd>");
        AccountId accId = new AccountId(Integer.parseInt(cmd[1]));
        Money amount = Money.of(new BigDecimal(cmd[2]));

        Account account = loadAccount(accId);
        account.withdraw(amount);
        saveAndProject(account);

        System.out.println("Withdrew " + amount + " from account " + accId);
    }

    // Implementation for transfer command (transactionally sound via two atomic events)
    private static void commandTransfer(String[] cmd) {
        if (cmd.length != 4) throw new IllegalArgumentException("Usage: transfer <from> <to> <usd>");
        AccountId fromId = new AccountId(Integer.parseInt(cmd[1]));
        AccountId toId = new AccountId(Integer.parseInt(cmd[2]));
        Money amount = Money.of(new BigDecimal(cmd[3]));

        // 1. Load both aggregates
        Account fromAccount = loadAccount(fromId);
        Account toAccount = loadAccount(toId);

        // 2. Execute command (emits MoneyTransferSend and MoneyTransferReceive events)
        fromAccount.transfer(toAccount, amount);

        // 3. Persistence: Save events from both streams
        saveAndProject(fromAccount);
        saveAndProject(toAccount);

        System.out.println("Transferred " + amount + " from " + fromId + " to " + toId);
    }

    // Implementation for balance command (queries the query port)
    private static void commandBalance(String[] cmd) {
        if (cmd.length != 2) throw new IllegalArgumentException("Usage: balance <acc>");
        AccountId accId = new AccountId(Integer.parseInt(cmd[1]));

        Money balance = queryPort.findBalanceByAccountId(accId);
        
        System.out.println("Balance for account " + accId + ": " + balance + " USD");
    }

    // Implementation for vvk_ command (queries the PostgreSQL read model)
    private static void commandVvk(String[] cmd) {
        if (cmd.length != 1) throw new IllegalArgumentException("Usage: vvk_");
        
        try {
            System.out.println("--- Account List (via Postgres Read Model) ---");
            System.out.println(readModel.listMasked());
            System.out.println("----------------------------------------------");
        } catch (java.sql.SQLException e) {
            // This is a system/connection error, the stack trace will be printed via the handler's catch block.
            throw new RuntimeException("Postgres Query Error: " + e.getMessage());
        }
    }


    /* ---------- Aggregate Loader & Event Saver ---------- */

    // Loads the account from the cache or rebuilds it from the event stream
    private static Account loadAccount(AccountId accId) {
        String key = accId.toString();
        if (cache.containsKey(key)) {
            return cache.get(key);
        }
        
        Account account = eventStore.loadAccount(accId);
        cache.put(key, account);
        return account;
    }

    // Saves events to event store and projects them to the read model
    private static void saveAndProject(Account a) {
        // Events are guaranteed to be in order
        a.getUncommittedEvents().forEach(evt -> {
            // 1. Save to Event Store (Write Model: Cosmos/Dynamo)
            eventStore.saveEvent(evt);
            
            // 2. Project to Read Model (Postgres)
            readModel.project(evt);
        });
        // Clear events after successful persistence
        a.markEventsAsCommitted();
    }


    /* ---------- Store Selector and Initializer (Prioritizes Cloud) ---------- */

    // Configures the correct EventStore based on environment variables
    private static AccountEventStorePort chooseEventStore() {
        String cloud = System.getenv("CLOUD");
        
        if ("aws".equals(cloud)) {
            System.out.println("Using AWS DynamoDB Event Store...");
            return new DynamoEventStore(DynamoDbClient.create(), "bank-events"); 
        }
        
        if ("azure".equals(cloud)) {
            System.out.println("Using Azure CosmosDB Event Store...");
            // Uses the CosmosEventStore you built
            return new CosmosEventStore(
                System.getenv("COSMOS_URI"),
                System.getenv("COSMOS_KEY"),
                "bankdb",
                "bank-events"
            );
        }
        
        // Default local fallback (ONLY if CLOUD variable is not set)
        System.out.println("Using In-Memory Account Store (volatile)...");
        return new InMemoryAccountStore(); 
    }

    // Initializes the Postgres Read Projection (MANDATORY, fails if env vars are missing)
    private static PostgresBalanceProjection initPostgres() {
        String url  = System.getenv("POSTGRES_URL");
        String user = System.getenv("POSTGRES_USER");
        String pass = System.getenv("POSTGRES_PASS");
        
        if (url == null || user == null || pass == null) {
            throw new IllegalStateException("Postgres environment variables (POSTGRES_URL, POSTGRES_USER, POSTGRES_PASS) are missing. Check your web ID env.");
        }
            
        try { 
            System.out.println("Initializing Postgres Read Projection...");
            return new PostgresBalanceProjection(url, user, pass); 
        }
        catch (Exception e) { 
            throw new RuntimeException("Failed to initialize Postgres Read Model: " + e.getMessage(), e); 
        }
    }


    /* ---------- 5-digit sequence implementation ---------- */
    private static final class AtomicSequence implements DatabaseSequence {
        private final AtomicInteger counter = new AtomicInteger(10_000);
        @Override 
        public int nextAcc() { 
            return counter.getAndIncrement(); 
        }
    }
}

