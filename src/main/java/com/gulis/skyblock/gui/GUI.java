package com.gulis.skyblock.gui;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Abstract base class for all inventory-based GUIs.
 *
 * <p>Minecraft GUIs are inventory-based (chest/dispenser interfaces rendered
 * by the client). They work identically on every OS — there is no "Linux GUI"
 * vs "Windows GUI". Subclasses implement {@link #build()} to populate the
 * inventory and {@link #handleClick(InventoryClickEvent)} to route clicks.</p>
 *
 * <p>This class also implements {@link InventoryHolder} so the inventory can be
 * tagged with the GUI instance, allowing the {@link GUIListener} to recover the
 * GUI from a click event without relying on title string matching (which breaks
 * with color codes and translations).</p>
 */
public abstract class GUI implements InventoryHolder {

    protected final Player player;
    protected final String title;
    protected final int size; // Must be a multiple of 9 (9-54)
    protected Inventory inventory;

    protected GUI(Player player, String title, int size) {
        this.player = player;
        this.title = ChatColor.translateAlternateColorCodes('&', title);
        this.size = size;
        // Pass `this` as the holder so the listener can recover the GUI instance
        this.inventory = Bukkit.createInventory(this, size, this.title);
    }

    /**
     * Populates the inventory with items. Called by {@link #open()} before
     * showing the inventory to the player.
     */
    public abstract void build();

    /**
     * Handles a click inside this GUI. The event is already cancelled by the
     * {@link GUIListener} to prevent item theft; subclasses decide what to do.
     *
     * @param event the click event
     */
    public abstract void handleClick(InventoryClickEvent event);

    /**
     * Builds the inventory, opens it for the player, and registers the GUI
     * with the {@link GUIManager} so clicks can be routed back here.
     */
    public void open() {
        build();
        player.openInventory(inventory);
    }

    /**
     * Rebuilds and refreshes the inventory contents without reopening it.
     */
    public void refresh() {
        inventory.clear();
        build();
        player.updateInventory();
    }

    /**
     * Helper to create a named item with optional lore.
     *
     * @param item the base item stack
     * @param name the display name (color codes supported)
     * @param lore the lore lines (color codes supported)
     * @return the decorated item stack
     */
    protected ItemStack createItem(ItemStack item, String name, String... lore) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
            if (lore.length > 0) {
                List<String> coloredLore = new ArrayList<>();
                for (String line : lore) {
                    coloredLore.add(ChatColor.translateAlternateColorCodes('&', line));
                }
                meta.setLore(coloredLore);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * Fills empty slots in the inventory with the given filler item.
     *
     * @param filler the item to fill empty slots with
     */
    protected void fillEmpty(ItemStack filler) {
        for (int i = 0; i < size; i++) {
            if (inventory.getItem(i) == null) {
                inventory.setItem(i, filler);
            }
        }
    }

    public Player getPlayer() {
        return player;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
