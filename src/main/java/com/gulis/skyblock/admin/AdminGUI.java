package com.gulis.skyblock.admin;

import com.gulis.skyblock.core.Skyblock;
import com.gulis.skyblock.gui.GUI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Statistic;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;

/**
 * Main admin panel GUI. Acts as the in-game control panel for the server.
 *
 * <p>Layout (54-slot chest):</p>
 * <pre>
 * Row 1: [Server Info] [Players] [World] [Maintenance]
 * Row 2: [Restart] [Save World] [Reload] [Whitelist]
 * Row 3: [Difficulty] [Time Day] [Time Night] [Weather Sun]
 * Row 4: [Weather Storm] [Gamemode] [TP to Me] [Close]
 * </pre>
 *
 * <p>Many items open sub-panels or trigger immediate actions. The GUI is
 * refreshed after each action so live data (TPS, player count, RAM) stays
 * current.</p>
 */
public class AdminGUI extends GUI {

    private final Skyblock plugin;

    public AdminGUI(Skyblock plugin, Player player) {
        super(player, "&4&lAdmin Panel", 54);
        this.plugin = plugin;
    }

    @Override
    public void build() {
        // --- Row 1: Information panels ---
        inventory.setItem(0, createInfoItem());
        inventory.setItem(1, createPlayersItem());
        inventory.setItem(2, createWorldItem());
        inventory.setItem(3, createMaintenanceItem());

        // --- Row 2: Server actions ---
        inventory.setItem(9, createItem(new ItemStack(Material.REDSTONE),
                "&cRestart Server",
                "&7Schedules a restart in 10 seconds.",
                "&7(Requires a wrapper script to auto-restart.)"));
        inventory.setItem(10, createItem(new ItemStack(Material.CHEST),
                "&aSave All Worlds",
                "&7Forces a world save."));
        inventory.setItem(11, createItem(new ItemStack(Material.BOOK),
                "&eReload Plugin Configs",
                "&7Reloads config.yml, shops.yml,",
                "&7messages.yml from disk."));
        inventory.setItem(12, createItem(new ItemStack(Material.NAME_TAG),
                "&fToggle Whitelist",
                "&7Currently: " + (Bukkit.hasWhitelist() ? "&aON" : "&cOFF")));

        // --- Row 3: World settings ---
        inventory.setItem(18, createItem(new ItemStack(Material.DIAMOND_SWORD),
                "&cToggle PvP",
                "&7Current: " + (plugin.getIslandManager().getIslandWorld() != null
                        && !plugin.getIslandManager().getIslandWorld().getPVP()
                        ? "&aDisabled" : "&cEnabled")));
        inventory.setItem(19, createItem(new ItemStack(Material.ZOMBIE_HEAD),
                "&6Difficulty: " + Bukkit.getWorlds().get(0).getDifficulty().name(),
                "&7Click to cycle difficulty."));
        inventory.setItem(20, createItem(new ItemStack(Material.SUNFLOWER),
                "&eSet Time: Day",
                "&7Sets the world time to noon."));
        inventory.setItem(21, createItem(new ItemStack(Material.BLACK_BED),
                "&1Set Time: Night",
                "&7Sets the world time to midnight."));

        // --- Row 4: Weather & misc ---
        inventory.setItem(27, createItem(new ItemStack(Material.SUNFLOWER),
                "&eWeather: Sun",
                "&7Clears weather in all worlds."));
        inventory.setItem(28, createItem(new ItemStack(Material.WATER_BUCKET),
                "&9Weather: Storm",
                "&7Starts a storm in all worlds."));
        inventory.setItem(29, createItem(new ItemStack(Material.COMMAND_BLOCK),
                "&dGamemode",
                "&7Cycles through Creative/Survival/Adventure/Spectator."));
        inventory.setItem(30, createItem(new ItemStack(Material.ENDER_PEARL),
                "&5Teleport to Me",
                "&7Teleports all online players to you."));

        // --- Bottom row: close + status ---
        ItemStack status = createItem(new ItemStack(Material.LIME_WOOL),
                "&aSystem OK",
                "&7TPS: " + getTPSString(),
                "&7RAM: " + getRAMString(),
                "&7Uptime: " + getUptimeString());
        inventory.setItem(45, status);

        ItemStack close = createItem(new ItemStack(Material.BARRIER), "&cClose", "&7Click to close.");
        inventory.setItem(49, close);
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        int slot = event.getRawSlot();
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        switch (slot) {
            case 0: // Server info — refresh to show live stats
                refresh();
                return;
            case 1: // Players sub-panel
                PlayerManagerGUI sub = new PlayerManagerGUI(plugin, player);
                sub.open();
                plugin.getGuiManager().register(player, sub);
                return;
            case 2: // World sub-panel
                ServerControlGUI worldGui = new ServerControlGUI(plugin, player, ServerControlGUI.Type.WORLD);
                worldGui.open();
                plugin.getGuiManager().register(player, worldGui);
                return;
            case 3: // Maintenance sub-panel
                ServerControlGUI maintGui = new ServerControlGUI(plugin, player, ServerControlGUI.Type.MAINTENANCE);
                maintGui.open();
                plugin.getGuiManager().register(player, maintGui);
                return;
            case 9: // Restart
                Bukkit.broadcastMessage(ChatColor.RED + "[Server] Restarting in 10 seconds...");
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    Bukkit.getWorlds().forEach(org.bukkit.World::save);
                    Bukkit.shutdown();
                }, 200L); // 10s
                return;
            case 10: // Save worlds
                Bukkit.getWorlds().forEach(org.bukkit.World::save);
                player.sendMessage(ChatColor.GREEN + "All worlds saved.");
                return;
            case 11: // Reload configs
                plugin.getConfigManager().reloadAll();
                plugin.getShopManager().load();
                player.sendMessage(ChatColor.GREEN + "Configs reloaded.");
                return;
            case 12: // Toggle whitelist
                boolean now = !Bukkit.hasWhitelist();
                Bukkit.setWhitelist(now);
                player.sendMessage(ChatColor.GREEN + "Whitelist " + (now ? "enabled" : "disabled") + ".");
                refresh();
                return;
            case 18: // Toggle PvP
                if (plugin.getIslandManager().getIslandWorld() != null) {
                    boolean pvp = !plugin.getIslandManager().getIslandWorld().getPVP();
                    plugin.getIslandManager().getIslandWorld().setPVP(pvp);
                    player.sendMessage(ChatColor.GREEN + "PvP " + (pvp ? "enabled" : "disabled") + ".");
                    refresh();
                }
                return;
            case 19: // Cycle difficulty
                org.bukkit.Difficulty current = Bukkit.getWorlds().get(0).getDifficulty();
                org.bukkit.Difficulty next = switch (current) {
                    case PEACEFUL -> org.bukkit.Difficulty.EASY;
                    case EASY -> org.bukkit.Difficulty.NORMAL;
                    case NORMAL -> org.bukkit.Difficulty.HARD;
                    case HARD -> org.bukkit.Difficulty.PEACEFUL;
                };
                for (org.bukkit.World w : Bukkit.getWorlds()) w.setDifficulty(next);
                player.sendMessage(ChatColor.GREEN + "Difficulty set to " + next.name() + ".");
                refresh();
                return;
            case 20: // Day
                for (org.bukkit.World w : Bukkit.getWorlds()) w.setTime(1000L);
                player.sendMessage(ChatColor.GREEN + "Time set to day.");
                return;
            case 21: // Night
                for (org.bukkit.World w : Bukkit.getWorlds()) w.setTime(14000L);
                player.sendMessage(ChatColor.GREEN + "Time set to night.");
                return;
            case 27: // Sun
                for (org.bukkit.World w : Bukkit.getWorlds()) {
                    w.setStorm(false);
                    w.setThundering(false);
                }
                player.sendMessage(ChatColor.GREEN + "Weather cleared.");
                return;
            case 28: // Storm
                for (org.bukkit.World w : Bukkit.getWorlds()) w.setStorm(true);
                player.sendMessage(ChatColor.GREEN + "Storm started.");
                return;
            case 29: // Gamemode
                org.bukkit.GameMode currentGm = player.getGameMode();
                org.bukkit.GameMode nextGm = switch (currentGm) {
                    case SURVIVAL -> org.bukkit.GameMode.CREATIVE;
                    case CREATIVE -> org.bukkit.GameMode.ADVENTURE;
                    case ADVENTURE -> org.bukkit.GameMode.SPECTATOR;
                    case SPECTATOR -> org.bukkit.GameMode.SURVIVAL;
                };
                player.setGameMode(nextGm);
                player.sendMessage(ChatColor.GREEN + "Gamemode set to " + nextGm.name() + ".");
                return;
            case 30: // Teleport all to me
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (!p.equals(player)) p.teleport(player);
                }
                player.sendMessage(ChatColor.GREEN + "All players teleported to you.");
                return;
            case 49: // Close
                player.closeInventory();
                return;
            default:
                break;
        }
    }

    private ItemStack createInfoItem() {
        List<String> lore = new ArrayList<>();
        lore.add("&7Players: &f" + Bukkit.getOnlinePlayers().size() + "/" + Bukkit.getMaxPlayers());
        lore.add("&7Worlds: &f" + Bukkit.getWorlds().size());
        lore.add("&7TPS: &f" + getTPSString());
        lore.add("&7RAM: &f" + getRAMString());
        lore.add("&7Uptime: &f" + getUptimeString());
        lore.add("&7Version: &f" + Bukkit.getBukkitVersion());
        return createItem(new ItemStack(Material.BOOK), "&bServer Info", lore.toArray(new String[0]));
    }

    private ItemStack createPlayersItem() {
        return createItem(new ItemStack(Material.PLAYER_HEAD),
                "&ePlayer Manager",
                "&7Online: &f" + Bukkit.getOnlinePlayers().size(),
                "&7Click to manage players.");
    }

    private ItemStack createWorldItem() {
        return createItem(new ItemStack(Material.GRASS_BLOCK),
                "&aWorld Control",
                "&7Time, weather, PvP, difficulty.",
                "&7Click to open.");
    }

    private ItemStack createMaintenanceItem() {
        return createItem(new ItemStack(Material.REDSTONE_TORCH),
                "&cMaintenance",
                "&7Whitelist, restart, reload.",
                "&7Click to open.");
    }

    /**
     * Returns a formatted TPS string (approximate, based on tick times).
     */
    private String getTPSString() {
        double tps = Bukkit.getTPS()[0];
        return String.format("%.1f", Math.min(20.0, tps));
    }

    /**
     * Returns used/total RAM in MB.
     */
    private String getRAMString() {
        Runtime runtime = Runtime.getRuntime();
        long used = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024);
        long max = runtime.maxMemory() / (1024 * 1024);
        return used + "MB / " + max + "MB";
    }

    /**
     * Returns a human-readable uptime string.
     */
    private String getUptimeString() {
        long ms = ManagementFactory.getRuntimeMXBean().getUptime();
        long seconds = ms / 1000;
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        return hours + "h " + minutes + "m";
    }
}
