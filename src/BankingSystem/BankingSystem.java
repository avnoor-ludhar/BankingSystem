package BankingSystem;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

// custom exception for an invalid input
class InvalidInputException extends Exception {
    public InvalidInputException(String message) {
        super(message);
    }
}

//custom exception for insufficient funds for a transaction
class InsufficientFundsException extends Exception {
    public InsufficientFundsException(String message) {
        super(message);
    }
}

//class to synchronize printing to the console
class ConsolePrinter {
    //static method for printing to the console
    public static synchronized void print(String message) {
        System.out.println(message);
    }
}

//Admin generic class with type extending the Account class
class Admin<A extends Account<Transaction>>{
    //Bank Initialized with type A for type safety
    private Bank<A> bank;
    //basic password for admin
    //NOTE: password should not be stored in code but for the scopr of this project it is a static variable
    private static final String PASSWORD = "admin123"; // Static password for admin

    //Admin constructor which has an instance of the current bank we have
    public Admin(Bank<A> bank) {
        this.bank = bank;
    }

    //authentication to ensure passsword matches to be able to use admin priveledges
    public static boolean authenticate(String inputPassword) {
        return PASSWORD.equals(inputPassword);
    }

    //moniter account method to print each user's account
    public void monitorAccounts() {
        ConsolePrinter.print("Monitoring all accounts:");
        //uses a forEach loop and calls the callback function on each account in the getAccounts map
        //Call back function returns the key and val for each entry in the map
        bank.getAccounts().forEach((accountNumber, account) -> {
            //prints the account via the toString method displaying polymorphism
            ConsolePrinter.print(account.toString());
            //another call back function to print each transaction via the forEach loop to iterate through all elements
            //in the transactions List
            account.getTransactions().forEach(transaction -> {
                //prints the transaction via the toString method
                ConsolePrinter.print("  - " + transaction);
            });
        });
    }

    //generate report method for the admin class which is just a wrapper method for the bank,generateReport() method
    public void generateReport() {
        bank.generateReport();
    }
}

// User class representing a bank customer
class User {
    private String fullName; // Full name of the customer
    private String username; // Username of the customer
    private String address; // Home address of the customer
    private String phoneNumber; // Phone number of the customer

    // Constructor to initialize user details
    public User(String fullName, String username, String address, String phoneNumber) {
        this.fullName = fullName;
        this.username = username;
        this.address = address;
        this.phoneNumber = phoneNumber;
    }

    // Getters and setters are below
    public String getFullName() {
        return fullName;
    }

    public String getUsername() {
        return username;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    // Override toString method to display user details in unique way
    @Override
    public String toString() {
        return String.format("Username: %s\nAddress: %s\nPhone: %s", username, address, phoneNumber);
    }
}

// Abstract Account class representing a bank account
abstract class Account<T extends Transaction> {
    private String accountNumber; // Unique account number
    private User accountHolder; // Account holder details
    protected double balance; // Account balance
    protected List<T> transactions; // List of transactions including initial deposit

    // Constructor to initialize account details
    public Account(String accountNumber, User accountHolder, double balance) {
        this.accountNumber = accountNumber;
        this.accountHolder = accountHolder;
        this.balance = balance;
        this.transactions = new ArrayList<>(); // Initialize transactions list

        // Add initial deposit as the first transaction
        this.transactions.add(createInitialDepositTransaction(accountNumber, balance));
    }

    //getter for the transactions history of the accounta
    public List<T> getTransactions() {
        return transactions;
    }

    // Getter method for account number
    public String getAccountNumber() {
        return accountNumber;
    }

    // Getter method for account holder
    public User getAccountHolder() {
        return accountHolder;
    }

    // Getter method for balance
    public double getBalance() {
        return balance;
    }

    // Abstract method for depositing money (using subclasses)
    public abstract void deposit(double amount);

    // Abstract method for withdrawing money
    public abstract boolean withdraw(double amount) throws InsufficientFundsException;

    // Method to add transaction to the list
    public void addTransaction(T transaction) {
        transactions.add(transaction);
    }

    // Abstract method to create an initial deposit transaction
    protected abstract T createInitialDepositTransaction(String accountNumber, double balance);

    // Override toString method to display account details
    @Override
    public String toString() {
        return String.format("Owner: %s\nAccount Number: %s\nType: %s\nBalance: $%.2f",
                accountHolder.getFullName(), accountNumber, this.getClass().getSimpleName(), balance);
    }
}


// CheckingAccount class representing a checking account
class CheckingAccount extends Account<Transaction> {

    // Constructor to initialize checking account details
    public CheckingAccount(String accountNumber, User accountHolder, double balance) {
        super(accountNumber, accountHolder, balance);
    }

    // Implement deposit method for checking account
    @Override
    public void deposit(double amount) {
        if (amount > 0) {
            balance += amount; // Increase balance by deposit amount
            transactions.add(new Transaction(getAccountNumber(), "Deposit", amount));
        }
    }

    // Implement withdraw method for checking account
    @Override
    public boolean withdraw(double amount) throws InsufficientFundsException {
        if (amount > 0 && amount <= balance) {
            balance -= amount; // Decrease balance by withdrawal amount
            transactions.add(new Transaction(getAccountNumber(), "Withdrawal", amount));
            return true;
        } else {
            throw new InsufficientFundsException("Insufficient funds or invalid amount.");
        }
    }

    //overrides method from abstract class for the initial deposit into an account
    @Override
    protected Transaction createInitialDepositTransaction(String accountNumber, double balance) {
        return new Transaction(accountNumber, "Initial Deposit", balance);
    }
}

//class to apply interest to a savings account on a specified interval
class InterestApplicator implements Runnable {
    //acount interest is being applied to
    private SavingsAccount account;

    //constructor to get the instance of the savings account
    public InterestApplicator(SavingsAccount account) {
        this.account = account;
    }

    //implementation of the run method for thread execution
    @Override
    public void run() {
        account.addInterest();
    }
}

//Savings account inherits from the Account class
class SavingsAccount extends Account<Transaction> {
    private double interestRate; // Interest rate for savings account
    private ScheduledExecutorService scheduler; //executer service which will run a thread at specified intervals

    // Constructor to initialize savings account details
    public SavingsAccount(String accountNumber, User accountHolder, double balance, double interestRate) {
        super(accountNumber, accountHolder, balance);
        //sets the interest rate and instaniates a scheduler
        this.interestRate = interestRate;
        this.scheduler = Executors.newScheduledThreadPool(1);
        //class that applies the interest
        InterestApplicator interestApplicator = new InterestApplicator(this);
        //sets the scheduler to apply interest every minute
        scheduler.scheduleAtFixedRate(interestApplicator, 0, 1, TimeUnit.MINUTES);
    }

    // Implement deposit method for savings account
    @Override
    public void deposit(double amount) {
        if (amount > 0) {
            balance += amount; // Increase balance by deposit amount
            transactions.add(new Transaction(getAccountNumber(), "Deposit", amount));
        }
    }

    // Implement withdraw method for savings account
    @Override
    public boolean withdraw(double amount) throws InsufficientFundsException {
        if (amount > 0 && amount <= balance) {
            balance -= amount; // Decrease balance by withdrawal amount
            transactions.add(new Transaction(getAccountNumber(), "Withdrawal", amount));
            return true;
        } else {
            throw new InsufficientFundsException("Insufficient funds or invalid amount.");
        }
    }


    // Method to add interest to the balance
    public void addInterest() {
        //adds the balance times the interest rate
        double interestAmount = balance * interestRate;
        balance += interestAmount; // Increase balance by interest amount
        //adds the transaction and prints to the console
        transactions.add(new Transaction(getAccountNumber(), "Interest", interestAmount));
//        ConsolePrinter.print(String.format("Interest added to account number: %s", getAccountNumber()));
    }

    //overrides the createInitialDepositTransaction in the Account abstract class
    @Override
    protected Transaction createInitialDepositTransaction(String accountNumber, double balance) {
        return new Transaction(accountNumber, "Initial Deposit", balance);
    }
}

// Transaction class representing a financial transaction
class Transaction {
    private String accountNumber; // Account number involved in the transaction
    private String transactionType; // Type of transaction (deposit/withdraw)
    private double amount; // Amount involved in the transaction
    private Date date; // Date of the transaction

    // Constructor to initialize transaction details
    public Transaction(String accountNumber, String transactionType, double amount) {
        this.accountNumber = accountNumber;
        this.transactionType = transactionType;
        this.amount = amount;
        this.date = new Date(); // Set transaction date to current date
    }

    // Getter for account number
    public String getAccountNumber() {
        return accountNumber;
    }

    public Date getDate() {
        return date;
    }

    // Getter for transaction type
    public String getTransactionType() {
        return transactionType;
    }

    // Getter for amount
    public double getAmount() {
        return amount;
    }

    // Override toString method to display transaction details
    @Override
    public String toString() {
        return String.format("Account Number: %s, Transaction Type: %s, Amount: %.2f, Date: %s", accountNumber, transactionType, amount, date);
    }
}

// Bank class representing the bank system
class Bank<A extends Account<Transaction>> {
    private final Map<String, A> accounts; // Map to store accounts with account number as key
    private final Map<String, User> users; // Map to store users with username as key

    // Constructor to initialize bank
    public Bank() {
        accounts = new HashMap<>(); // Initialize accounts map
        users = new HashMap<>(); // Initialize users map
    }

    //method to get all accounts in the bank
    public Map<String, A> getAccounts() {
        return accounts;
    }

    //method for conducting all the admin actions
    public void adminActions(Admin admin, Scanner scanner) {
        //admin password
        System.out.print("Enter admin password: ");
        String password = scanner.nextLine();

        //First authenticates the admin via the password passed in
        if (!Admin.authenticate(password)) {
            ConsolePrinter.print("Invalid password. Access denied.");
            return;
        }

        //CLI for admin actions after login is successful
        while (true) {
            //print statements
            ConsolePrinter.print("\nAdmin Actions:");
            ConsolePrinter.print("1. Monitor Accounts");
            ConsolePrinter.print("2. Generate Report");
            ConsolePrinter.print("3. Exit");
            System.out.print("\nPlease select an option (1-3): ");

            try {
                //type of the operation we want to do as an admin
                int choice = scanner.nextInt();
                scanner.nextLine();

                //switch statement for each operation
                switch (choice) {
                    case 1:
                        admin.monitorAccounts();
                        break;
                    case 2:
                        admin.generateReport();
                        break;
                    case 3:
                        return;
                    default:
                        //throws an invalid input exception to state that is not a valid option
                        throw new InvalidInputException("Invalid option. Please select again.");
                }
            } catch (InputMismatchException e) {
                //print statement for InputMismatchError
                ConsolePrinter.print("Invalid input. Please enter a number between 1 and 3.");
                scanner.nextLine(); // Clear the invalid input
            } catch (InvalidInputException e) {
                //print statement for the invalidInputException we created
                ConsolePrinter.print(e.getMessage());
            }
        }
    }

    // Method to open a new account
    public void openAccount(String username, String accountNumber, double initialDeposit, String accountType) {
        User accountHolder = users.get(username); // Retrieve user details from users map
        //checks if it is a valid account and the account is not already added to the account map
        if (accountHolder != null && !accounts.containsKey(accountNumber)) {
            //we then create a new checking account or savings account and type cast it to the generic type to be added to the map
            A newAccount;
            if (accountType.equalsIgnoreCase("checking")) {
                newAccount = (A) new CheckingAccount(accountNumber, accountHolder, initialDeposit);
            } else if (accountType.equalsIgnoreCase("savings")) {
                newAccount = (A) new SavingsAccount(accountNumber, accountHolder, initialDeposit, 0.02);
            } else {
                ConsolePrinter.print("Invalid account type.");
                return;
            }
            accounts.put(accountNumber, newAccount); // Add new account to accounts map
            ConsolePrinter.print("Account opened successfully with account number: " + accountNumber);
        } else {
            ConsolePrinter.print("Failed to open account. Please check your input.");
        }
    }

    //checks if the user is in the users map
    public boolean containsUser(String username) {
        return users.containsKey(username);
    }

    //performs a transaction asynchronously via a thread
    public Thread performTransaction(String accountNumber, String transactionType, double amount) {
        //implementation of the runnable interface via lambda expression
        Runnable processTransaction = () -> {
            //gets the account number we are doing a transaction on
            A account = accounts.get(accountNumber);
            if (account != null) {
                //synchronizes the account to ensure no other processes are occuring on the shared account field
                synchronized (account) {
                    try {
                        //switch case for the different transaction types
                        switch (transactionType.toLowerCase()) {
                            case "deposit":
                                //calls the deposit method
                                account.deposit(amount);
                                ConsolePrinter.print(String.format("Transaction successful! New balance: %.2f\n", account.getBalance()));
                                break;
                            case "withdrawal":
                                //calls withdraw method
                                boolean success = account.withdraw(amount);
                                //returns the state of the withdraw
                                if (success) {
                                    ConsolePrinter.print(String.format("Transaction successful! New balance: %.2f\n", account.getBalance()));
                                }
                                break;
                            case "transfer":
                                //prompts for the destination account number
                                System.out.print("Enter destination account number: ");
                                Scanner scanner = new Scanner(System.in);
                                String destAccountNumber = scanner.nextLine();
                                //calls the transfer method on the specified variables
                                transfer(accountNumber, destAccountNumber, amount);
                                break;
                            default:
                                //throws an error if the transaction type is not correct
                                throw new InvalidInputException("Invalid transaction type.");
                        }
                    } catch (InsufficientFundsException | InvalidInputException e) {
                        //catching exceptions
                        ConsolePrinter.print(e.getMessage());
                    }
                }
            } else {
                ConsolePrinter.print("Account not found. Please check your input.");
            }
        };

        //returns the Thread
        return new Thread(processTransaction);
    }

    // Method to transfer money between accounts
    private void transfer(String fromAccountNumber, String toAccountNumber, double amount) throws InsufficientFundsException, InvalidInputException {
        //gets the 2 accounts from the map via the account numbers passed in
        A fromAccount = accounts.get(fromAccountNumber);
        A toAccount = accounts.get(toAccountNumber);

        if (fromAccount != null && toAccount != null) {
            //withdraws the amount if possible
            if (fromAccount.withdraw(amount)) {
                toAccount.deposit(amount);
                ConsolePrinter.print(String.format("$%.2f transferred from account %s to account %s.\n",
                        amount, fromAccountNumber, toAccountNumber));
            } else{
                //throws an exception if there are not enough funds
                throw new InsufficientFundsException("Insufficient funds for the transfer.");
            }
        } else {
            throw new InvalidInputException("One or both accounts not found. Please check your input.");
        }
    }

    // Method to search for accounts by name, username, or account number
    public void searchAccounts(String keyword) {
        //boolean to see if the account exists
        boolean found = false;
        //iterates through each account in the accounts map values
        for (A account : accounts.values()) {
            //checks if the full name matches, username matches, or account number matches
            if (account.getAccountHolder().getFullName().toLowerCase().contains(keyword.toLowerCase()) ||
                    account.getAccountHolder().getUsername().toLowerCase().contains(keyword.toLowerCase()) ||
                    account.getAccountNumber().toLowerCase().contains(keyword.toLowerCase())) {
                //prints content from the account
                ConsolePrinter.print("Owner: " + account.getAccountHolder().getFullName());
                ConsolePrinter.print("Account Number: " + account.getAccountNumber());
                ConsolePrinter.print("Type: " + account.getClass().getSimpleName());
                ConsolePrinter.print(String.format("Balance: $%.2f\n\n", account.getBalance()));
                found = true;
            }
        }
        //prints error if account is not found
        if (!found) {
            ConsolePrinter.print("No accounts found matching the search criteria.");
        }
    }

    //method to asynchronously generate a report of all the accounts and transactions
    public void generateReport() {
        //lambda expression to implmenent the runnable interface inline
        new Thread(() -> {
            //opens the FileWriter in append mode
            try (FileWriter writer = new FileWriter("accounts_report.txt", true)) {
                //prints that the report sequence has started
                ConsolePrinter.print("Report is being generated asynchronously.");
                //writing the title to the file
                writer.write("Accounts Report:\n");
                writer.write("======================================\n");

                //synchonizes the accounts variable to ensure access to most accurate account information
                synchronized (accounts) {
                    //iterates through the accounts
                    for (A account : accounts.values()) {
                        //writes the information to the file with a formatted string
                        writer.write(String.format("Owner: %s\n", account.getAccountHolder().getFullName()));
                        writer.write(String.format("Account Number: %s\n", account.getAccountNumber()));
                        writer.write(String.format("Type: %s\n", account.getClass().getSimpleName()));
                        writer.write(String.format("Balance: %.2f\n", account.getBalance()));

                        //prints transactions
                        writer.write("Transactions:\n");
                        //synchronizes the account to ensure no other transactions happen during the process
                        synchronized (account) {
                            for (Transaction transaction : account.transactions) {
                                //writes the transaction to the file
                                writer.write(String.format("  - %s: %.2f on %s\n", transaction.getTransactionType(), transaction.getAmount(), transaction.getDate()));
                            }
                        }

                        writer.write("======================================\n");
                    }
                }

                //print statement
                ConsolePrinter.print("Report generated and saved as 'accounts_report.txt'");
            } catch (IOException e) {
                //catches error
                ConsolePrinter.print("Error generating report: " + e.getMessage());
            }
        }).start();
    }

    //method to update the account information of a user
    public void updateAccountInformation(Scanner scanner) {
        try {
            //gets the account via the account number
            System.out.print("Enter account number: ");
            String accountNumber = scanner.nextLine();
            //gets the account
            A account = accounts.get(accountNumber);
            //if no account is found then an exception is thrown
            if (account == null) {
                throw new InvalidInputException("Account not found.");
            }

            //prompts user for the address and phone number
            System.out.print("Enter new address (leave blank to keep current): ");
            String address = scanner.nextLine();
            if (!address.isEmpty() && !address.matches("^\\d+\\s+\\w+\\s+\\w+$")) {
                throw new InvalidInputException("Address must be in the format: 'number word word'. Example: '123 Main Street'.");
            }
            System.out.print("Enter new phone number (leave blank to keep current): ");
            String phoneNumber = scanner.nextLine();
            validatePhoneNumber(phoneNumber);

            //updates the address and phone number
            if (!address.isEmpty()) {
                account.getAccountHolder().setAddress(address);
            }
            if (!phoneNumber.isEmpty()) {
                account.getAccountHolder().setPhoneNumber(phoneNumber);
            }

            ConsolePrinter.print("Account information updated successfully.");
        } catch (InvalidInputException e) {
            ConsolePrinter.print(e.getMessage());
        }
    }

    //method to display the main CLI for the application
    public void showMainMenu() {
        //initializes the scanner and the admin
        Scanner scanner = new Scanner(System.in);
        Admin admin = new Admin(this); // Create an Admin instance with the current Bank instance

        //main method for the CLI
        while (true) {
            //print statements for the options
            ConsolePrinter.print("\nWelcome to the Bank Management System!");
            ConsolePrinter.print("------------------------------------------------");
            ConsolePrinter.print("1. Register New Customer");
            ConsolePrinter.print("2. Open Account");
            ConsolePrinter.print("3. Perform Transaction");
            ConsolePrinter.print("4. View Account");
            ConsolePrinter.print("5. Search Accounts");
            ConsolePrinter.print("6. Generate Report");
            ConsolePrinter.print("7. Update Account Information");
            ConsolePrinter.print("8. Admin Actions");
            ConsolePrinter.print("9. Exit");
            System.out.print("\nPlease select an option (1-9): ");

            try {
                int choice = scanner.nextInt();
                scanner.nextLine();

                //switch statement that calls the respective functionality dependant on the input
                switch (choice) {
                    case 1:
                        registerNewCustomer(scanner);
                        break;
                    case 2:
                        openAccount(scanner);
                        break;
                    case 3:
                        performTransaction(scanner);
                        break;
                    case 4:
                        viewAccount(scanner);
                        break;
                    case 5:
                        searchAccounts(scanner);
                        break;
                    case 6:
                        generateReport();
                        break;
                    case 7:
                        updateAccountInformation(scanner);
                        break;
                    case 8:
                        adminActions(admin, scanner);
                        break;
                    case 9:
                        //exit message and closes the scanner and program
                        ConsolePrinter.print("Thank you for using the Bank Management System.");
                        scanner.close();
                        System.exit(0);
                    default:
                        //throws an error for an invalid option
                        throw new InvalidInputException("Invalid option. Please select again.");
                }
            } catch (InputMismatchException e) {
                //error message for a non integer input
                ConsolePrinter.print("Invalid input. Please enter a number between 1 and 9.");
                scanner.nextLine(); // Clear the invalid input
            } catch (InvalidInputException e) {
                //prints the custon exception
                ConsolePrinter.print(e.getMessage());
            }
        }
    }

    // Method to register a new customer
    private void registerNewCustomer(Scanner scanner) {
        try {
            //gets the customers full name
            System.out.print("Enter customer full name: ");
            String fullName = scanner.nextLine();
            validateFullName(fullName);
            //checks if it was not inputted
            if (fullName.isEmpty()) {
                throw new InvalidInputException("Full name is required.");
            }

            //gets a username and ensures it was passed in
            System.out.print("Enter username: ");
            String username = scanner.nextLine();
            if (username.isEmpty()) {
                throw new InvalidInputException("Username is required.");
            }
            //checks if the username already exists since it must be unique for the search
            if (users.containsKey(username)) {
                throw new InvalidInputException("Username already exists. Please choose a different username.");
            }

            //gets an address and ensrures it was passed in
            System.out.print("Enter address: ");
            String address = scanner.nextLine();
            if (address.isEmpty() || !address.matches("^\\d+\\s+\\w+\\s+\\w+$")) {
                throw new InvalidInputException("Address must be in the format: 'number word word'. Example: '123 Main Street'.");
            }

            //gets a phone number and
            System.out.print("Enter phone number: ");
            String phoneNumber = scanner.nextLine();
            validatePhoneNumber(phoneNumber);

            //creates a new User object and adds it to the users map
            User newUser = new User(fullName, username, address, phoneNumber);
            users.put(username, newUser);
            ConsolePrinter.print("Customer registered successfully!");
        } catch (InvalidInputException e) {
            //prints exception message
            ConsolePrinter.print(e.getMessage());
        }
    }

    private void validatePhoneNumber(String phoneNumber) throws InvalidInputException {
        if (phoneNumber.length() != 10) {
            throw new InvalidInputException("Phone number must be exactly 10 digits long. Try again.");
        }

        int digitCount = 0;
        for (char ch : phoneNumber.toCharArray()) {
            if (Character.isDigit(ch)) {
                digitCount++;
            } else {
                throw new InvalidInputException("Phone number can only contain digits. Try again.");
            }
        }

        if (digitCount != 10) {
            throw new InvalidInputException("Phone number must be exactly 10 digits long. Try again.");
        }
    }


    // Method to validate full name
    private void validateFullName(String fullName) throws InvalidInputException {
        if (!fullName.matches("[a-zA-Z\\s]+")) {
            throw new InvalidInputException("Full name can only contain letters. Try again.");
        }
    }

    // Method to open a new account
    private void openAccount(Scanner scanner) {
        try {
            //gets the user name and ensures
            System.out.print("Enter customer username: ");
            String username = scanner.nextLine();

            //checks if the user is in the users map
            if (!this.containsUser(username)) {
                throw new InvalidInputException("Username not found.");
            }

            //checks the account type we would like to open
            System.out.print("Select account type (Checking/Savings): ");
            String accountType = scanner.nextLine();

            //ensures its a valid account type
            if (!accountType.equalsIgnoreCase("checking") && !accountType.equalsIgnoreCase("savings")) {
                throw new InvalidInputException("Must input a checking or savings account");
            }

            //initial deposit into the account
            System.out.print("Initial deposit: ");
            double initialDeposit = scanner.nextDouble();
            scanner.nextLine();

            //if the initial deposit is negative or 0 we throw an exception
            if (initialDeposit <= 0) {
                throw new InvalidInputException("Initial deposit must be greater than zero.");
            }

            //generates an account number and opens the account with the user
            String accountNumber = generateAccountNumber();
            openAccount(username, accountNumber, initialDeposit, accountType);
        } catch (InputMismatchException e) {
            //print error for exception
            ConsolePrinter.print("Invalid input. Please enter a valid amount.");
            scanner.nextLine(); // Clear the invalid input
        } catch (InvalidInputException e) {
            //prints error message
            ConsolePrinter.print(e.getMessage());
        }
    }

    // Method to perform a transaction
    private void performTransaction(Scanner scanner) {
        try {
            //gets the account number from the user
            System.out.print("Enter account number: ");
            String accountNumber = scanner.nextLine();
            //checks if the account exists with that account number
            if (!accounts.containsKey(accountNumber)) {
                //exception thrown
                throw new InvalidInputException("Account number is required.");
            }

            //asks for the transaction type
            System.out.print("Select transaction type (Deposit/Withdrawal/Transfer): ");
            String transactionType = scanner.nextLine();
            //check if the user inputted a valid type
            if (!transactionType.equalsIgnoreCase("deposit") &&
                    !transactionType.equalsIgnoreCase("withdrawal") &&
                    !transactionType.equalsIgnoreCase("transfer")) {
                throw new InvalidInputException("Invalid transaction type. Must be Deposit, Withdrawal, or Transfer.");
            }

            //asks for amount
            System.out.print("Amount: ");
            double amount = scanner.nextDouble();
            scanner.nextLine(); // Consume newline

            //checks if it is a valid amount
            if (amount <= 0) {
                throw new InvalidInputException("Transaction amount must be greater than zero.");
            }

            // checks if the account has enough funds in the case we are doing a transfer of withdrawal
            if(amount > accounts.get(accountNumber).getBalance() && (transactionType.equalsIgnoreCase("withdrawal") || transactionType.equalsIgnoreCase("transfer"))){
                throw new InvalidInputException("Insufficient funds.");
            }

            try{
                //gets the transaction thread to run
                Thread t = performTransaction(accountNumber, transactionType, amount);
                //starts the thread and ensures execution before moving on
                t.start();
                t.join();
            }catch(InterruptedException e){
                //prints exception
                ConsolePrinter.print(e.getMessage());
            }

        } catch (InputMismatchException e) {
            ConsolePrinter.print("Invalid input. Please enter a valid amount.");
            scanner.nextLine(); // Clear the invalid input
        } catch (InvalidInputException e) {
            //prints exception
            ConsolePrinter.print(e.getMessage());
        }
    }

    // Method to view account details
    private void viewAccount(Scanner scanner) {
        try {
            //prompts and ensures the account exists
            System.out.print("Enter account number: ");
            String accountNumber = scanner.nextLine();
            if (!accounts.containsKey(accountNumber)) {
                throw new InvalidInputException("Account number is incorrect.");
            }

            //finds the account from the map
            A account = accounts.get(accountNumber);
            if (account != null) {
                //prints account details from the toString method
                ConsolePrinter.print("Account Details:");
                ConsolePrinter.print(account.toString());

                //prints the transaction history from the account
                ConsolePrinter.print("Transactions:");
                //iterates through each transaction in the account
                for (Transaction transaction : account.transactions) {
                    //prints each transaction
                    ConsolePrinter.print("- " + transaction.getTransactionType() + " $" + transaction.getAmount());
                }
            } else {
                //thorws an exception if the account isn't found
                throw new InvalidInputException("Account not found. Please check your input.");
            }
        } catch (InvalidInputException e) {
            //prints custom exception
            ConsolePrinter.print(e.getMessage());
        }
    }

    // wrapper method to prompt user for the search term
    private void searchAccounts(Scanner scanner) {
        System.out.print("Enter name, username or account number to search: ");
        String keyword = scanner.nextLine();

        searchAccounts(keyword);
    }

    // Method to generate a unique account number
    private String generateAccountNumber() {
        //uses StringBuilder to create the account number via the Random class
        Random random = new Random();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 9; i++) {
            //adds a random integer one by one to create a 9 digit account number
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }
}


// Main class to run the bank management system
public class BankingSystem {
    public static void main(String[] args) {
        Bank<Account<Transaction>> bank = new Bank<Account<Transaction>>();
        bank.showMainMenu();
    }
}