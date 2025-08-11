import java.util.*;

public class ATMApp {
    public static void main(String[] args) {
        ATM atm = new ATM();
        atm.seedDemoAccounts();
        atm.run();
    }
}

// ---------- Account class ----------
class Account {
    private final String accountNumber;
    private String pin;
    private double balance;
    private final String ownerName;
    private final Deque<Transaction> transactions;

    public Account(String accountNumber, String pin, double initialBalance, String ownerName) {
        this.accountNumber = accountNumber;
        this.pin = pin;
        this.balance = initialBalance;
        this.ownerName = ownerName;
        this.transactions = new ArrayDeque<>();
        addTransaction("Account opened", initialBalance);
    }

    public String getAccountNumber() { return accountNumber; }
    public String getOwnerName() { return ownerName; }
    public boolean verifyPin(String attempt) { return pin.equals(attempt); }
    public void changePin(String newPin) { this.pin = newPin; }
    public double getBalance() { return balance; }

    public synchronized boolean deposit(double amount) {
        if (amount <= 0) return false;
        balance += amount;
        addTransaction("Deposit", amount);
        return true;
    }

    public synchronized boolean withdraw(double amount) {
        if (amount <= 0 || amount > balance) return false;
        balance -= amount;
        addTransaction("Withdraw", -amount);
        return true;
    }

    public synchronized boolean transferOut(double amount, String toAccountNumber) {
        if (amount <= 0 || amount > balance) return false;
        balance -= amount;
        addTransaction("Transfer to " + toAccountNumber, -amount);
        return true;
    }

    public synchronized void transferIn(double amount, String fromAccountNumber) {
        balance += amount;
        addTransaction("Transfer from " + fromAccountNumber, amount);
    }

    private void addTransaction(String type, double amount) {
        Transaction t = new Transaction(new Date(), type, amount, balance);
        transactions.addFirst(t);
        if (transactions.size() > 20) transactions.removeLast();
    }

    public List<Transaction> getMiniStatement(int count) {
        List<Transaction> list = new ArrayList<>();
        int c = 0;
        for (Transaction t : transactions) {
            if (c++ >= count) break;
            list.add(t);
        }
        return list;
    }
}

// ---------- Transaction class ----------
class Transaction {
    private final Date timestamp;
    private final String type;
    private final double amount;
    private final double postBalance;

    public Transaction(Date timestamp, String type, double amount, double postBalance) {
        this.timestamp = timestamp;
        this.type = type;
        this.amount = amount;
        this.postBalance = postBalance;
    }

    @Override
    public String toString() {
        return String.format("%tF %tT | %-18s | %9.2f | Bal: %8.2f",
                timestamp, timestamp, type, amount, postBalance);
    }
}

// ---------- ATM class ----------
class ATM {
    private final Scanner scanner = new Scanner(System.in);
    private final Map<String, Account> accounts = new HashMap<>();
    private Account currentAccount = null;

    public void seedDemoAccounts() {
        accounts.put("1001", new Account("1001", "1234", 5000.00, "Alice"));
        accounts.put("1002", new Account("1002", "2222", 15000.50, "Bob"));
        accounts.put("1003", new Account("1003", "3333", 250.75, "Charlie"));
    }

    public void run() {
        System.out.println("=== Welcome to Simple Java ATM ===");
        while (true) {
            if (currentAccount == null) {
                showLoginMenu();
            } else {
                showMainMenu();
            }
        }
    }

    private void showLoginMenu() {
        System.out.println("\n1) Login\n2) Exit");
        System.out.print("Choose: ");
        String choice = scanner.nextLine().trim();
        switch (choice) {
            case "1": loginFlow(); break;
            case "2": System.out.println("Goodbye!"); System.exit(0);
            default: System.out.println("Invalid choice. Try again.");
        }
    }

    private void loginFlow() {
        System.out.print("Enter account number: ");
        String accNum = scanner.nextLine().trim();
        Account acc = accounts.get(accNum);
        if (acc == null) {
            System.out.println("Account not found.");
            return;
        }
        System.out.print("Enter 4-digit PIN: ");
        String pin = scanner.nextLine().trim();
        if (acc.verifyPin(pin)) {
            currentAccount = acc;
            System.out.println("Login successful. Welcome, " + acc.getOwnerName() + "!");
        } else {
            System.out.println("Incorrect PIN.");
        }
    }

    private void showMainMenu() {
        System.out.println("\n--- Main Menu ---");
        System.out.println("1) View Balance");
        System.out.println("2) Deposit");
        System.out.println("3) Withdraw");
        System.out.println("4) Transfer");
        System.out.println("5) Mini-statement");
        System.out.println("6) Change PIN");
        System.out.println("7) Logout");
        System.out.print("Choose: ");
        String choice = scanner.nextLine().trim();
        switch (choice) {
            case "1": viewBalance(); break;
            case "2": depositFlow(); break;
            case "3": withdrawFlow(); break;
            case "4": transferFlow(); break;
            case "5": miniStatementFlow(); break;
            case "6": changePinFlow(); break;
            case "7": logout(); break;
            default: System.out.println("Invalid choice. Try again.");
        }
    }

    private void viewBalance() {
        System.out.printf("Current balance: %.2f\n", currentAccount.getBalance());
    }

    private void depositFlow() {
        System.out.print("Enter amount to deposit: ");
        Double amount = parsePositiveAmount(scanner.nextLine());
        if (amount == null) {
            System.out.println("Invalid amount.");
            return;
        }
        if (currentAccount.deposit(amount))
            System.out.printf("Deposited %.2f. New balance: %.2f\n", amount, currentAccount.getBalance());
        else
            System.out.println("Deposit failed.");
    }

    private void withdrawFlow() {
        System.out.print("Enter amount to withdraw: ");
        Double amount = parsePositiveAmount(scanner.nextLine());
        if (amount == null) {
            System.out.println("Invalid amount.");
            return;
        }
        if (currentAccount.withdraw(amount))
            System.out.printf("Withdrawn %.2f. New balance: %.2f\n", amount, currentAccount.getBalance());
        else
            System.out.println("Withdrawal failed. Check balance or amount.");
    }

    private void transferFlow() {
        System.out.print("Enter destination account number: ");
        String destAccNum = scanner.nextLine().trim();
        if (destAccNum.equals(currentAccount.getAccountNumber())) {
            System.out.println("Cannot transfer to same account.");
            return;
        }
        Account dest = accounts.get(destAccNum);
        if (dest == null) {
            System.out.println("Destination account not found.");
            return;
        }
        System.out.print("Enter amount to transfer: ");
        Double amount = parsePositiveAmount(scanner.nextLine());
        if (amount == null) {
            System.out.println("Invalid amount.");
            return;
        }
        synchronized (this) {
            if (!currentAccount.transferOut(amount, destAccNum)) {
                System.out.println("Transfer failed. Check balance or amount.");
                return;
            }
            dest.transferIn(amount, currentAccount.getAccountNumber());
        }
        System.out.printf("Transferred %.2f to %s. New balance: %.2f\n",
                amount, dest.getOwnerName(), currentAccount.getBalance());
    }

    private void miniStatementFlow() {
        System.out.print("How many recent transactions? (default 5): ");
        String s = scanner.nextLine().trim();
        int count = 5;
        if (!s.isEmpty()) {
            try { count = Math.max(1, Integer.parseInt(s)); } catch (NumberFormatException ignored) {}
        }
        List<Transaction> list = currentAccount.getMiniStatement(count);
        System.out.println("\n--- Mini-statement ---");
        if (list.isEmpty()) System.out.println("No transactions.");
        else list.forEach(System.out::println);
    }

    private void changePinFlow() {
        System.out.print("Enter current PIN: ");
        if (!currentAccount.verifyPin(scanner.nextLine().trim())) {
            System.out.println("Incorrect current PIN.");
            return;
        }
        System.out.print("Enter new 4-digit PIN: ");
        String p1 = scanner.nextLine().trim();
        if (!p1.matches("\\d{4}")) {
            System.out.println("Invalid PIN format.");
            return;
        }
        System.out.print("Confirm new PIN: ");
        String p2 = scanner.nextLine().trim();
        if (!p1.equals(p2)) {
            System.out.println("PINs do not match.");
            return;
        }
        currentAccount.changePin(p1);
        System.out.println("PIN changed successfully.");
    }

    private void logout() {
        System.out.println("Logged out: " + currentAccount.getOwnerName());
        currentAccount = null;
    }

    private Double parsePositiveAmount(String s) {
        try {
            double val = Double.parseDouble(s);
            return val > 0 ? Math.round(val * 100.0) / 100.0 : null;
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}

