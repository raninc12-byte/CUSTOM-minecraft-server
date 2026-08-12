package com.gulis.skyblock.shop;

import com.gulis.skyblock.core.Skyblock;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles the {@code /shop}, {@code /buy}, {@code /sell} and {@code /sellall}
 * commands. All four are registered to this executor in {@link Skyblock#onEnable()}.
 */
public class ShopCommand implements CommandExecutor, TabCompleter {

    private final Skyblock plugin;

    public ShopCommand(Skyblock plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Shop commands can only be used by players.");
            return true;
        }
        Player player = (Player) sender;
        switch (command.getName().toLowerCase()) {
            case "shop":
                openShop(player);
                return true;
            case "buy":
                return handleBuy(player, args);
            case "sell":
                return handleSell(player, args);
            case "sellall":
                return handleSellAll(player);
            default:
                return false;
        }
    }

    private void openShop(Player player) {
        ShopGUI gui = new ShopGUI(plugin, player);
        gui.open();
        plugin.getGuiManager().register(player, gui);
    }

    private boolean handleBuy(Player player, String[] args) {
        if (args.length < 1) {
            player.sendMessage(ChatColor.RED + "Usage: /buy <item> [amount]");
            return true;
        }
        Material material = Material.matchMaterial(args[0]);
        if (material == null || material == Material.AIR) {
            player.sendMessage(ChatColor.RED + "Unknown item: " + args[0]);
            return true;
        }
        if (!plugin.getShopManager().isBuyable(material)) {
            player.sendMessage(ChatColor.RED + "That item is not available for purchase.");
            return true;
        }
        int amount = 1;
        if (args.length >= 2) {
            try {
                amount = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                player.sendMessage(ChatColor.RED + "Invalid amount: " + args[1]);
                return true;
            }
        }
        if (amount <= 0) {
            player.sendMessage(ChatColor.RED + "Amount must be greater than zero.");
            return true;
        }

        double unitPrice = plugin.getShopManager().getBuyPrice(material);
        double total = unitPrice * amount;
        if (!plugin.getEconomyManager().has(player, total)) {
            player.sendMessage(ChatColor.RED + "You cannot afford that. Need "
                    + plugin.getEconomyManager().format(total) + ".");
            return true;
        }

        ItemStack stack = new ItemStack(material, amount);
        java.util.Map<Integer, ItemStack> overflow = player.getInventory().addItem(stack);
        if (!overflow.isEmpty()) {
            int notAdded = overflow.values().stream().mapToInt(ItemStack::getAmount).sum();
            int actual = amount - notAdded;
            if (actual <= 0) {
                player.sendMessage(ChatColor.RED + "Your inventory is full!");
                return true;
            }
            total = unitPrice * actual;
            amount = actual;
        }
        plugin.getEconomyManager().withdraw(player, total);
        player.sendMessage(ChatColor.GREEN + "Bought " + amount + "x "
                + material.name() + ChatColor.GREEN + " for "
                + ChatColor.GOLD + plugin.getEconomyManager().format(total) + ChatColor.GREEN + ".");
        return true;
    }

    private boolean handleSell(Player player, String[] args) {
        if (args.length < 1) {
            player.sendMessage(ChatColor.RED + "Usage: /sell <item> [amount]");
            return true;
        }
        Material material = Material.matchMaterial(args[0]);
        if (material == null || material == Material.AIR) {
            player.sendMessage(ChatColor.RED + "Unknown item: " + args[0]);
            return true;
        }
        if (!plugin.getShopManager().isSellable(material)) {
            player.sendMessage(ChatColor.RED + "That item cannot be sold to the shop.");
            return true;
        }
        int available = countItem(player, material);
        if (available <= 0) {
            player.sendMessage(ChatColor.RED + "You do not have any " + material.name() + ".");
            return true;
        }
        int amount = available;
        if (args.length >= 2) {
            try {
                amount = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                player.sendMessage(ChatColor.RED + "Invalid amount: " + args[1]);
                return true;
            }
        }
        amount = Math.min(amount, available);
        if (amount <= 0) {
            player.sendMessage(ChatColor.RED + "Amount must be greater than zero.");
            return true;
        }

        removeItem(player, material, amount);
        double total = plugin.getShopManager().getSellPrice(material) * amount;
        plugin.getEconomyManager().deposit(player, total);
        player.sendMessage(ChatColor.GREEN + "Sold " + amount + "x "
                + material.name() + ChatColor.GREEN + " for "
                + ChatColor.GOLD + plugin.getEconomyManager().format(total) + ChatColor.GREEN + ".");
        return true;
    }

    private boolean handleSellAll(Player player) {
        double total = 0.0;
        int totalItems = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null || item.getType() == Material.AIR) continue;
            double sellPrice = plugin.getShopManager().getSellPrice(item.getType());
            if (sellPrice <= 0) continue;
            total += sellPrice * item.getAmount();
            totalItems += item.getAmount();
        }
        if (totalItems == 0) {
            player.sendMessage(ChatColor.RED + "You have no sellable items.");
            return true;
        }
        // Actually remove the items now
        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null || item.getType() == Material.AIR) continue;
            if (plugin.getShopManager().isSellable(item.getType())) {
                player.getInventory().removeItem(item);
            }
        }
        plugin.getEconomyManager().deposit(player, total);
        player.sendMessage(ChatColor.GREEN + "Sold " + totalItems + " items for "
                + ChatColor.GOLD + plugin.getEconomyManager().format(total) + ChatColor.GREEN + ".");
        return true;
    }

    private int countItem(Player player, Material material) {
        int count = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == material) {
                count += item.getAmount();
            }
        }
        return count;
    }

    private void removeItem(Player player, Material material, int amount) {
        int remaining = amount;
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length && remaining > 0; i++) {
            ItemStack item = contents[i];
            if (item == null || item.getType() != material) continue;
            if (item.getAmount() <= remaining) {
                remaining -= item.getAmount();
                player.getInventory().setItem(i, null);
            } else {
                item.setAmount(item.getAmount() - remaining);
                remaining = 0;
            }
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> suggestions = new ArrayList<>();
        if (args.length != 1) return suggestions;
        String prefix = args[0].toUpperCase();
        for (Material material : Material.values()) {
            if (material.isItem() && material.name().startsWith(prefix)) {
                suggestions.add(material.name().toLowerCase());
            }
        }
        return suggestions;
    }
}
