package com.vvk.banque.cli;

import com.vvk.banque.domain.AggregatesObj.Account;
import com.vvk.banque.domain.ValueObj.*;
import com.vvk.banque.adapter.persistence.InMemoryAccountStore;

import java.math.BigDecimal;
import java.util.*;

public final class BankShell {

    private static final Scanner in = new Scanner(System.in);
    private static final InMemoryAccountStore store = new InMemoryAccountStore();
    private static final Map<String, Account> cache = new HashMap<>();

    public static void main(String[] args) {
        while (true) {
            String[] cmd = in.nextLine().trim().split("\\s+");
            if (cmd.length == 0) continue;
            try {
                switch (cmd[0]) {
                    case "open"     -> open(cmd);
                    case "deposit"  -> deposit(cmd);
                    case "withdraw" -> withdraw(cmd);
                    case "transfer" -> transfer(cmd);
                    case "balance"  -> balance(cmd);
                    case "history"  -> history(cmd);
                    case "exit"     -> { return; }
                    default         -> System.out.println("unknown");
                }
            } catch (Exception ex) {
                System.out.println("err: " + ex.getMessage());
            }
        }
    }

    /* ---------- command handlers ---------- */
    private static void open(String[] c) {
        AccountId aid = AccountId.generateUnique(() -> Integer.parseInt(c[1])); // quick hack
        CustomerId cid = CustomerId.generate(c[2]);
        Money m = new Money(new BigDecimal(c[3]), Currency.getInstance("USD"));
        Account acc = Account.open(aid, cid, m);
        cache.put(aid.toString(), acc);
        acc.getUncommittedEvents().forEach(store::saveEvent);
        acc.markEventsAsCommitted();
        System.out.println("opened " + aid);
    }

    private static void deposit(String[] c) {
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

    private static void balance(String[] c) {
        Account a = load(c[1]);
        System.out.println("balance " + a.getBalance().getAmt());
    }

    private static void history(String[] c) {
        AccountId aid = AccountId.fromString(c[1]);
        System.out.println("events " + store.loadEvents(aid).size());
    }

    /* ---------- helpers ---------- */
    private static Account load(String raw) {
        AccountId aid = AccountId.fromString(raw);
        return cache.computeIfAbsent(raw, k -> store.loadAccount(aid));
    }

    private static void commit(Account a) {
        a.getUncommittedEvents().forEach(store::saveEvent);
        a.markEventsAsCommitted();
    }
}
