package com.gulis.skyblock.economy;

import com.gulis.skyblock.core.Skyblock;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

/**
 * Manages player balances backed by the SQLite {@code players} table.
 *
 * <p>Each player's balance is cached in memory after first access to avoid
 * hitting the database on every transaction. Writes are flushed immediately so
 * a crash never loses money. The starting balance is read from {@code config.yml}.</p>
 */
public class EconomyManager {

    private final Skyblock plugin;
    private final java.util.Map<UUID, Double> cache = new java.util.concurrent.ConcurrentHashMap<>();
    private double startingBalance;

    public EconomyManager(Skyblock plugin) {
        this.plugin = plugin;
        this.startingBalance = plugin.getConfigManager().getConfig()
                .getDouble("economy.starting-balance", 100.0);
    }

    /**
     * Returns a player's balance, loading it from the database on first access.
     *
     * @param player the player
     * @return the balance (never null)
     */
    public double getBalance(OfflinePlayer player) {
        UUID uuid = player.getUniqueId();
        if (cache.containsKey(uuid)) {
            return cache.get(uuid);
        }
        double balance = loadBalance(uuid);
        cache.put(uuid, balance);
        return balance;
    }

    /**
     * Loads a balance from the database, creating the row if missing.
     */
    private double loadBalance(UUID uuid) {
        try (ResultSet rs = plugin.getDatabaseManager().query(
                "SELECT balance FROM players WHERE uuid = ?", uuid.toString())) {
            if (rs.next()) {
                return rs.getDouble("balance");
            }
            // First-time player: insert with starting balance
            plugin.getDatabaseManager().update(
                    "INSERT INTO players (uuid, balance) VALUES (?, ?)",
                    uuid.toString(), startingBalance);
            return startingBalance;
        } catch (SQLException e) {
            plugin.getLogger().warning("Could not load balance for " + uuid + ": " + e.getMessage());
            return 0.0;
        }
    }

    /**
     * Sets a player's balance to an exact amount.
     *
     * @param player the player
     * @param amount the new balance (must be >= 0)
     */
    public void setBalance(OfflinePlayer player, double amount) {
        UUID uuid = player.getUniqueId();
        amount = Math.max(0.0, amount);
        cache.put(uuid, amount);
        try {
            plugin.getDatabaseManager().update(
                    "UPDATE players SET balance = ?, name = ? WHERE uuid = ?",
                    amount, player.getName(), uuid.toString());
        } catch (SQLException e) {
            plugin.getLogger().warning("Could not save balance for " + uuid + ": " + e.getMessage());
        }
    }

    /**
     * Adds money to a player's balance.
     *
     * @param player the player
     * @param amount the amount to add (must be > 0)
     */
    public void deposit(OfflinePlayer player, double amount) {
        setBalance(player, getBalance(player) + amount);
    }

    /**
     * Removes money from a player's balance.
     *
     * @param player the player
     * @param amount the amount to remove
     * @return true if the player had enough money and it was removed
     */
    public boolean withdraw(OfflinePlayer player, double amount) {
        double balance = getBalance(player);
        if (balance < amount) {
            return false;
        }
        setBalance(player, balance - amount);
        return true;
    }

    /**
     * Checks whether a player can afford a given amount.
     *
     * @param player the player
     * @param amount the amount
     * @return true if the balance is at least {@code amount}
     */
    public boolean has(OfflinePlayer player, double amount) {
        return getBalance(player) >= amount;
    }

    /**
     * Transfers money from one player to another.
     *
     * @param from the sender
     * @param to    the recipient
     * @param amount the amount
     * @return true if the transfer succeeded
     */
    public boolean transfer(Player from, OfflinePlayer to, double amount) {
        if (amount <= 0) {
            return false;
        }
        if (!withdraw(from, amount)) {
            return false;
        }
        deposit(to, amount);
        return true;
    }

    /**
     * Formats a monetary amount using the configured currency symbol and
     * decimal places.
     *
     * @param amount the raw amount
     * @return a formatted string like "$100.00"
     */
    public String format(double amount) {
        String symbol = plugin.getConfigManager().getConfig()
                .getString("economy.currency-symbol", "$");
        int decimals = plugin.getConfigManager().getConfig()
                .getInt("economy.decimals", 2);
        String format = "%." + decimals + "f";
        return symbol + String.format(format, amount);
    }
}
