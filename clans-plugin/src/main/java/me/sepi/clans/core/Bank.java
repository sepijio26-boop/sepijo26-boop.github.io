package me.sepi.clans.core;

import java.util.UUID;

/**
 * Minimal economy abstraction (Vault-backed in production, fake in tests).
 */
public interface Bank {

    boolean isAvailable();

    double balance(UUID player);

    boolean withdraw(UUID player, double amount);

    boolean deposit(UUID player, double amount);

    String format(double amount);
}
