package com.gulis.skyblock.economy;

import com.gulis.skyblock.core.Skyblock;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles the {@code /balance} and {@code /pay} commands.
 *
 * <p>Both commands are registered to this executor in {@link Skyblock#onEnable()}.
 * The command name is used to switch behavior.</p>
 */
public class EconomyCommand implements CommandExecutor, TabCompleter {

    private final Skyblock plugin;

    public EconomyCommand(Skyblock plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String name = command.getName().toLowerCase();
        switch (name) {
            case "balance":
                return handleBalance(sender, args);
            case "pay":
                return handlePay(sender, args);
            default:
                return false;
        }
    }

    private boolean handleBalance(CommandSender sender, String[] args) {
        OfflinePlayer target;
        if (args.length == 0) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("Console must specify a player: /balance <player>");
                return true;
            }
            target = (Player) sender;
        } else {
            target = Bukkit.getOfflinePlayer(args[0]);
            if (target == null || (!target.hasPlayedBefore() && !target.isOnline())) {
                sender.sendMessage(ChatColor.RED + "Player not found: " + args[0]);
                return true;
            }
        }

        double balance = plugin.getEconomyManager().getBalance(target);
        String formatted = plugin.getEconomyManager().format(balance);
        if (args.length == 0) {
            sender.sendMessage(ChatColor.GREEN + "Your balance: " + ChatColor.GOLD + formatted);
        } else {
            sender.sendMessage(ChatColor.GREEN + target.getName() + "'s balance: "
                    + ChatColor.GOLD + formatted);
        }
        return true;
    }

    private boolean handlePay(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use /pay.");
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /pay <player> <amount>");
            return true;
        }

        Player from = (Player) sender;
        OfflinePlayer to = Bukkit.getOfflinePlayer(args[0]);
        if (to == null || (!to.hasPlayedBefore() && !to.isOnline())) {
            sender.sendMessage(ChatColor.RED + "Player not found: " + args[0]);
            return true;
        }
        if (to.getUniqueId().equals(from.getUniqueId())) {
            sender.sendMessage(ChatColor.RED + "You cannot pay yourself.");
            return true;
        }

        double amount;
        try {
            amount = Double.parseDouble(args[1]);
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Invalid amount: " + args[1]);
            return true;
        }
        if (amount <= 0) {
            sender.sendMessage(ChatColor.RED + "Amount must be greater than zero.");
            return true;
        }

        if (!plugin.getEconomyManager().transfer(from, to, amount)) {
            sender.sendMessage(ChatColor.RED + "You do not have enough money.");
            return true;
        }

        String formatted = plugin.getEconomyManager().format(amount);
        sender.sendMessage(ChatColor.GREEN + "Paid " + ChatColor.GOLD + formatted
                + ChatColor.GREEN + " to " + to.getName() + ".");
        if (to.isOnline()) {
            ((Player) to).sendMessage(ChatColor.GREEN + "You received " + ChatColor.GOLD
                    + formatted + ChatColor.GREEN + " from " + from.getName() + ".");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (command.getName().equalsIgnoreCase("pay") && args.length == 1) {
            List<String> names = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase().startsWith(args[0].toLowerCase())) {
                    names.add(p.getName());
                }
            }
            return names;
        }
        return new ArrayList<>();
    }
}
