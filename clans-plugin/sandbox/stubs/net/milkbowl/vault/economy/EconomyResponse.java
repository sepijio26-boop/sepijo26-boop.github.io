package net.milkbowl.vault.economy;
public class EconomyResponse {
    public double amount;
    public double balance;
    public boolean transactionSuccess;
    public String errorMessage;
    public EconomyResponse(double amount, double balance, TransactionType type, String errorMessage) {
        this.amount = amount; this.balance = balance; this.transactionSuccess = type == TransactionType.SUCCESS; this.errorMessage = errorMessage;
    }
    public boolean transactionSuccess() { return transactionSuccess; }
    public String errorMessage() { return errorMessage; }
    public enum TransactionType { SUCCESS, FAILURE }
}
