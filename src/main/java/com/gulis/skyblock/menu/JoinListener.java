package com.gulis.skyblock.menu;

import com.gulis.skyblock.core.Skyblock;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Handles player join/respawn/interact events for the hub and main menu.
 *
 * <p>On join, players are teleported to the hub spawn and given a "Menu"
 * compass item. Right-clicking the compass opens the {@link MainMenuGUI}.
 * On respawn (after death) players return to the hub rather than their bed.</p>
 */
public class JoinListener implements Listener {

    private final Skyblock plugin;
    private static final int MENU_SLOT = 0;

    public JoinListener(Skyblock plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Teleport to the hub on first join (and always for ops-less players)
        if (plugin.getHubManager() != null && plugin.getHubManager().getSpawnLocation() != null) {
            // Only teleport if the player is new or is currently in the void
            if (!player.hasPlayedBefore() || player.getLocation().getY() < 0) {
                player.teleport(plugin.getHubManager().getSpawnLocation());
            }
        }

        // Give the menu compass
        giveMenuItem(player);

        player.sendMessage(ChatColor.GOLD + "Welcome " + player.getName() + "!");
        player.sendMessage(ChatColor.YELLOW + "Right-click the compass in your hotbar to open the menu.");
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        // Send players back to the hub on respawn
        if (plugin.getHubManager() != null && plugin.getHubManager().getSpawnLocation() != null) {
            Location hub = plugin.getHubManager().getSpawnLocation();
            event.setRespawnLocation(hub);
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getType() != Material.COMPASS) {
            return;
        }
        if (!isMenuItem(item)) {
            return;
        }
        event.setCancelled(true);
        MainMenuGUI gui = new MainMenuGUI(plugin, player);
        gui.open();
        plugin.getGuiManager().register(player, gui);
    }

    /**
     * Gives the player a compass that opens the main menu on right-click.
     * The item is placed in slot 0 of the hotbar if empty.
     */
    public static void giveMenuItem(Player player) {
        ItemStack compass = new ItemStack(Material.COMPASS);
        ItemMeta meta = compass.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GOLD + "" + ChatColor.BOLD + "Main Menu");
            meta.setLore(java.util.Collections.singletonList(
                    ChatColor.YELLOW + "Right-click to open the server menu."));
            compass.setItemMeta(meta);
        }
        // Only give it if the player doesn't already have one
        for (ItemStack invItem : player.getInventory().getContents()) {
            if (invItem != null && invItem.getType() == Material.COMPASS && isMenuItem(invItem)) {
                return; // already has one
            }
        }
        player.getInventory().setItem(MENU_SLOT, compass);
    }

    /**
     * Checks whether an item stack is the menu compass by its display name.
     */
    private static boolean isMenuItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.hasDisplayName()
                && meta.getDisplayName().equals(ChatColor.GOLD + "" + ChatColor.BOLD + "Main Menu");
    }
}
