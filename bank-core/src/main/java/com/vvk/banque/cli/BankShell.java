// src/main/java/com/vvk/banque/cli/BankShell.java
package com.vvk.banque.cli;

import com.vvk.banque.adapter.event.ConsoleEventPublisher;
import com.vvk.banque.adapter.persistence.InMemoryAccountStore;
import com.vvk.banque.app.services.*;
import com.vvk.banque.app.ports.in.*;
import com.vvk.banque.domain.ValueObj.AccountId;
import com.vvk.banque.domain.ValueObj.Money;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Scanner;
import java.util.UUID;
import java.util.Collections;

public class BankShell {

    private final DepositMoneyCommand depositHandler;
    private final WithdrawMoneyCommand withdrawHandler;
    private final TransferMoneyCommand transferHandler;
    private final CheckBalanceQuery balanceQuery;
    private final Currency currency;
    private final Scanner scanner = new Scanner(System.in);

    public BankShell() {
        this.currency = Currency.getInstance("INR"); // or "USD"/ "EUR" etc.
        InMemoryAccountStore store = new InMemoryAccountStore();
        ConsoleEventPublisher publisher = new ConsoleEventPublisher();

        this.depositHandler = new DepositMoneyCommandHandler(store, publisher);
        this.withdrawHandler = new WithdrawMoneyCommandHandler(store, publisher);
        this.transferHandler = new TransferMoneyCommandHandler(store, publisher);
        this.balanceQuery = new CheckBalanceQueryService(store);
    }

    public static void main(String[] args) {
        new BankShell().run();
    }

    public void run() {
        System.out.println("=== ATLAS Bank CLI ===");
        System.out.println("Note: This demo uses in-memory storage. Data resets on exit.");

        // Create a sample account for demo: AccountId requires (int sequence, UUID)
        AccountId demoAcc = new AccountId(10002, UUID.randomUUID());
        depositHandler.executeDepositMoney(demoAcc, new Money(new BigDecimal("1000"), currency));

        while (true) {
            showMenu();
            int choice = getIntInput();
            switch (choice) {
                case 1 -> System.out.println("Create customer: Not implemented in MVP");
                case 2 -> System.out.println("Open account: Not implemented in MVP");
                case 3 -> handleDeposit(demoAcc);
                case 4 -> handleWithdraw(demoAcc);
                case 5 -> handleTransfer(demoAcc);
                case 6 -> handleBalanceCheck(demoAcc);
                case 7 -> {
                    System.out.println("Goodbye!");
                    return;
                }
                default -> System.out.println("Invalid choice. Try again.");
            }
        }
    }

    private void showMenu() {
        System.out.println("\n[1] Create customer");
        System.out.println("[2] Open account");
        System.out.println("[3] Deposit");
        System.out.println("[4] Withdraw");
        System.out.println("[5] Transfer");
        System.out.println("[6] Check balance");
        System.out.println("[7] Exit");
        System.out.print("Choose an option: ");
    }

    private void handleDeposit(AccountId accId) {
        System.out.print("Enter amount: ");
        BigDecimal amt = getBigDecimalInput();
        if (amt != null && amt.compareTo(BigDecimal.ZERO) > 0) {
            depositHandler.executeDepositMoney(accId, new Money(amt, currency));
            System.out.println("Deposit successful.");
        } else {
            System.out.println("Invalid amount.");
        }
    }

    private void handleWithdraw(AccountId accId) {
        System.out.print("Enter amount: ");
        BigDecimal amt = getBigDecimalInput();
        if (amt != null && amt.compareTo(BigDecimal.ZERO) > 0) {
            try {
                withdrawHandler.executeWithdrawMoney(accId, new Money(amt, currency));
                System.out.println("Withdrawal successful.");
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
        } else {
            System.out.println("Invalid amount.");
        }
    }

    private void handleTransfer(AccountId fromAcc) {
        System.out.println("Creating a second account for transfer demo...");
        AccountId toAcc = new AccountId(2, UUID.randomUUID());
        depositHandler.executeDepositMoney(toAcc, new Money(new BigDecimal("500"), currency));

        System.out.print("Enter amount to transfer: ");
        BigDecimal amt = getBigDecimalInput();
        if (amt != null && amt.compareTo(BigDecimal.ZERO) > 0) {
            try {
                transferHandler.executeTransferMoney(fromAcc, toAcc, new Money(amt, currency));
                System.out.println("Transfer successful.");
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
        } else {
            System.out.println("Invalid amount.");
        }
    }

    private void handleBalanceCheck(AccountId accId) {
        Money balance = balanceQuery.executeCheckBalance(accId);
        System.out.println("Current balance: " + balance.getAmt() + " " + balance.getCur().getCurrencyCode());
    }

    private int getIntInput() {
        try {
            return Integer.parseInt(scanner.nextLine().trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private BigDecimal getBigDecimalInput() {
        try {
            return new BigDecimal(scanner.nextLine().trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
