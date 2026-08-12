package com.gulis.skyblock.admin;

import com.gulis.skyblock.core.Skyblock;
import com.gulis.skyblock.gui.GUI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Sub-panel for world control and maintenance actions.
 *
 * <p>This GUI has two modes selected via the {@link Type} enum. The world
 * mode focuses on time/weather/PvP, while the maintenance mode focuses on
 * whitelist, restart, and reload.</p>
 */
public class ServerControlGUI extends GUI {

    public enum Type { WORLD, MAINTENANCE }

    private final Skyblock plugin;
    private final Type type;

    public ServerControlGUI(Skyblock plugin, Player player, Type type) {
        super(player, type == Type.WORLD ? "&a&lWorld Control" : "&c&lMaintenance", 36);
        this.plugin = plugin;
        this.type = type;
    }

    @Override
    public void build() {
        // Top row: back + filler
        inventory.setItem(0, createItem(new ItemStack(Material.ARROW), "&a← Back", "&7Return to admin panel."));
        for (int i = 1; i < 9; i++) inventory.setItem(i, createItem(new ItemStack(Material.GRAY_STAINED_GLASS_PANE), "&7"));

        if (type == Type.WORLD) {
            inventory.setItem(10, createItem(new ItemStack(Material.SUNFLOWER), "&eTime: Day"));
            inventory.setItem(11, createItem(new ItemStack(Material.CLOCK), "&7Time: Noon"));
            inventory.setItem(12, createItem(new ItemStack(Material.BLACK_BED), "&1Time: Night"));
            inventory.setItem(13, createItem(new ItemStack(Material.ENDER_EYE), "&5Time: Midnight"));

            inventory.setItem(19, createItem(new ItemStack(Material.LIGHT_BLUE_DYE), "&bWeather: Sun"));
            inventory.setItem(20, createItem(new ItemStack(Material.GRAY_DYE), "&7Weather: Rain"));
            inventory.setItem(21, createItem(new ItemStack(Material.LIGHT_GRAY_DYE), "&8Weather: Thunder"));

            inventory.setItem(28, createItem(new ItemStack(Material.DIAMOND_SWORD), "&cToggle PvP"));
            inventory.setItem(29, createItem(new ItemStack(Material.ZOMBIE_HEAD),
                    "&6Difficulty: " + Bukkit.getWorlds().get(0).getDifficulty().name()));
            inventory.setItem(30, createItem(new ItemStack(Material.GRASS_BLOCK), "&aSave All Worlds"));
        } else {
            inventory.setItem(10, createItem(new ItemStack(Material.NAME_TAG),
                    "&fWhitelist: " + (Bukkit.hasWhitelist() ? "&aON" : "&cOFF")));
            inventory.setItem(11, createItem(new ItemStack(Material.BARRIER),
                    "&cReload Plugin Configs"));
            inventory.setItem(12, createItem(new ItemStack(Material.REDSTONE),
                    "&4Restart Server", "&7In 10 seconds."));
            inventory.setItem(13, createItem(new ItemStack(Material.REDSTONE_TORCH),
                    "&cStop Server", "&7Stops immediately."));
            inventory.setItem(14, createItem(new ItemStack(Material.COMMAND_BLOCK),
                    "&dRun Console Command", "&7See console for usage."));

            inventory.setItem(28, createItem(new ItemStack(Material.PUFFERFISH),
                    "&eClear All Inventories", "&7Resets every online player's inventory."));
            inventory.setItem(29, createItem(new ItemStack(Material.BEACON),
                    "&bHeal All Players"));
            inventory.setItem(30, createItem(new ItemStack(Material.LIGHTNING_ROD),
                    "&eStrike Lightning", "&7Where you are looking."));
        }

        inventory.setItem(27, createItem(new ItemStack(Material.BARRIER), "&cClose", ""));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        int slot = event.getRawSlot();
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        if (slot == 0) {
            AdminGUI admin = new AdminGUI(plugin, player);
            admin.open();
            plugin.getGuiManager().register(player, admin);
            return;
        }
        if (slot == 27) {
            player.closeInventory();
            return;
        }

        if (type == Type.WORLD) {
            switch (slot) {
                case 10: setTime(1000L); return;
                case 11: setTime(6000L); return;
                case 12: setTime(14000L); return;
                case 13: setTime(18000L); return;
                case 19:
                    for (org.bukkit.World w : Bukkit.getWorlds()) { w.setStorm(false); w.setThundering(false); }
                    player.sendMessage(ChatColor.GREEN + "Weather: Sun.");
                    return;
                case 20:
                    for (org.bukkit.World w : Bukkit.getWorlds()) w.setStorm(true);
                    player.sendMessage(ChatColor.GREEN + "Weather: Rain.");
                    return;
                case 21:
                    for (org.bukkit.World w : Bukkit.getWorlds()) { w.setStorm(true); w.setThundering(true); }
                    player.sendMessage(ChatColor.GREEN + "Weather: Thunder.");
                    return;
                case 28:
                    if (plugin.getIslandManager().getIslandWorld() != null) {
                        boolean pvp = !plugin.getIslandManager().getIslandWorld().getPVP();
                        plugin.getIslandManager().getIslandWorld().setPVP(pvp);
                        player.sendMessage(ChatColor.GREEN + "PvP " + (pvp ? "ON" : "OFF") + ".");
                        refresh();
                    }
                    return;
                case 29:
                    org.bukkit.Difficulty current = Bukkit.getWorlds().get(0).getDifficulty();
                    org.bukkit.Difficulty next = switch (current) {
                        case PEACEFUL -> org.bukkit.Difficulty.EASY;
                        case EASY -> org.bukkit.Difficulty.NORMAL;
                        case NORMAL -> org.bukkit.Difficulty.HARD;
                        case HARD -> org.bukkit.Difficulty.PEACEFUL;
                    };
                    for (org.bukkit.World w : Bukkit.getWorlds()) w.setDifficulty(next);
                    player.sendMessage(ChatColor.GREEN + "Difficulty: " + next.name() + ".");
                    refresh();
                    return;
                case 30:
                    Bukkit.getWorlds().forEach(org.bukkit.World::save);
                    player.sendMessage(ChatColor.GREEN + "Worlds saved.");
                    return;
                default:
                    break;
            }
        } else {
            switch (slot) {
                case 10:
                    boolean now = !Bukkit.hasWhitelist();
                    Bukkit.setWhitelist(now);
                    player.sendMessage(ChatColor.GREEN + "Whitelist " + (now ? "ON" : "OFF") + ".");
                    refresh();
                    return;
                case 11:
                    plugin.getConfigManager().reloadAll();
                    plugin.getShopManager().load();
                    player.sendMessage(ChatColor.GREEN + "Configs reloaded.");
                    return;
                case 12:
                    Bukkit.broadcastMessage(ChatColor.RED + "[Server] Restarting in 10 seconds...");
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        Bukkit.getWorlds().forEach(org.bukkit.World::save);
                        Bukkit.shutdown();
                    }, 200L);
                    return;
                case 13:
                    Bukkit.broadcastMessage(ChatColor.RED + "[Server] Stopping...");
                    Bukkit.getScheduler().runTaskLater(plugin, Bukkit::shutdown, 40L);
                    return;
                case 14:
                    player.sendMessage(ChatColor.YELLOW + "Use the console for arbitrary commands.");
                    return;
                case 28:
                    for (Player p : Bukkit.getOnlinePlayers()) p.getInventory().clear();
                    player.sendMessage(ChatColor.GREEN + "All inventories cleared.");
                    return;
                case 29:
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        p.setHealth(20.0);
                        p.setFoodLevel(20);
                    }
                    player.sendMessage(ChatColor.GREEN + "All players healed.");
                    return;
                case 30:
                    org.bukkit.block.Block target = player.getTargetBlock(null, 100);
                    if (target != null) {
                        target.getWorld().strikeLightning(target.getLocation());
                        player.sendMessage(ChatColor.GREEN + "Lightning struck.");
                    }
                    return;
                default:
                    break;
            }
        }
    }

    private void setTime(long time) {
        for (org.bukkit.World w : Bukkit.getWorlds()) w.setTime(time);
        player.sendMessage(ChatColor.GREEN + "Time set to " + time + ".");
    }
}
