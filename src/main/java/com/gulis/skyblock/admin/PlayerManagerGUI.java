package com.gulis.skyblock.admin;

import com.gulis.skyblock.core.Skyblock;
import com.gulis.skyblock.gui.GUI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Sub-panel listing online players with per-player actions.
 *
 * <p>Each player head in the inventory is clickable to teleport to them,
 * kick them, or (if op) ban them. The GUI is paginated for servers with many
 * players.</p>
 */
public class PlayerManagerGUI extends GUI {

    private final Skyblock plugin;
    private int page = 0;
    private static final int PER_PAGE = 28; // rows 1-4 cols 1-7

    public PlayerManagerGUI(Skyblock plugin, Player player) {
        super(player, "&4&lPlayer Manager", 54);
        this.plugin = plugin;
    }

    @Override
    public void build() {
        // Top row: back + title
        inventory.setItem(0, createItem(new ItemStack(Material.ARROW), "&a← Back", "&7Return to admin panel."));
        for (int i = 1; i < 9; i++) inventory.setItem(i, createItem(new ItemStack(Material.GRAY_STAINED_GLASS_PANE), "&7"));

        List<Player> online = new ArrayList<>(Bukkit.getOnlinePlayers());
        int start = page * PER_PAGE;
        int end = Math.min(start + PER_PAGE, online.size());

        int slot = 10;
        for (int i = start; i < end; i++) {
            Player target = online.get(i);
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            ItemMeta meta = head.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.GREEN + target.getName());
                List<String> lore = new ArrayList<>();
                lore.add(ChatColor.GRAY + "Health: " + ChatColor.RED + (int) target.getHealth() + "/20");
                lore.add(ChatColor.GRAY + "World: " + ChatColor.WHITE + target.getWorld().getName());
                lore.add(ChatColor.GRAY + "Gamemode: " + ChatColor.WHITE + target.getGameMode().name());
                lore.add(ChatColor.GRAY + "Balance: " + ChatColor.GOLD
                        + plugin.getEconomyManager().format(plugin.getEconomyManager().getBalance(target)));
                lore.add("");
                lore.add(ChatColor.YELLOW + "Left-click: " + ChatColor.WHITE + "Teleport to player");
                lore.add(ChatColor.YELLOW + "Right-click: " + ChatColor.WHITE + "Kick player");
                lore.add(ChatColor.YELLOW + "Shift-click: " + ChatColor.WHITE + "Toggle gamemode");
                meta.setLore(lore);
                head.setItemMeta(meta);
            }
            inventory.setItem(slot, head);
            slot++;
            if ((slot - 10) % 7 == 0) slot += 2; // skip border
        }

        // Bottom row: pagination + close
        int totalPages = Math.max(1, (int) Math.ceil((double) online.size() / PER_PAGE));
        if (page > 0) {
            inventory.setItem(45, createItem(new ItemStack(Material.ARROW), "&a← Previous", "&7Page " + page));
        }
        inventory.setItem(49, createItem(new ItemStack(Material.PAPER), "&fPage " + (page + 1) + "/" + totalPages));
        if (page < totalPages - 1) {
            inventory.setItem(53, createItem(new ItemStack(Material.ARROW), "&aNext →", "&7Page " + (page + 2)));
        }
        inventory.setItem(48, createItem(new ItemStack(Material.BARRIER), "&cClose", ""));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        int slot = event.getRawSlot();
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        switch (slot) {
            case 0: // Back
                AdminGUI admin = new AdminGUI(plugin, player);
                admin.open();
                plugin.getGuiManager().register(player, admin);
                return;
            case 45: // Prev
                if (page > 0) { page--; refresh(); }
                return;
            case 53: // Next
                int totalPages = Math.max(1, (int) Math.ceil((double) Bukkit.getOnlinePlayers().size() / PER_PAGE));
                if (page < totalPages - 1) { page++; refresh(); }
                return;
            case 48: // Close
                player.closeInventory();
                return;
            default:
                break;
        }

        // Player head click
        if (clicked.getType() == Material.PLAYER_HEAD) {
            List<Player> online = new ArrayList<>(Bukkit.getOnlinePlayers());
            int relative = slot - 10;
            int row = relative / 9;
            int col = relative % 9;
            if (col >= 7) return;
            int index = page * PER_PAGE + row * 7 + col;
            if (index < 0 || index >= online.size()) return;
            Player target = online.get(index);
            if (target.equals(player)) return;

            if (event.isShiftClick()) {
                // Toggle gamemode
                org.bukkit.GameMode newGm = target.getGameMode() == org.bukkit.GameMode.SURVIVAL
                        ? org.bukkit.GameMode.CREATIVE : org.bukkit.GameMode.SURVIVAL;
                target.setGameMode(newGm);
                player.sendMessage(ChatColor.GREEN + target.getName() + "'s gamemode set to "
                        + newGm.name() + ".");
                refresh();
            } else if (event.isRightClick()) {
                target.kickPlayer(ChatColor.RED + "Kicked by admin.");
                player.sendMessage(ChatColor.GREEN + "Kicked " + target.getName() + ".");
                refresh();
            } else {
                player.teleport(target);
                player.sendMessage(ChatColor.GREEN + "Teleported to " + target.getName() + ".");
            }
        }
    }
}
