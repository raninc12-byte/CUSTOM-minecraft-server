package com.gulis.skyblock.gui;

import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks which GUI each player currently has open.
 *
 * <p>Because each player can only have one inventory open at a time, a simple
 * {@code UUID -> GUI} map is sufficient. The map is concurrent because GUI
 * open/close events can fire from different threads (e.g. async chat handling
 * triggering a GUI open).</p>
 */
public class GUIManager {

    private final Map<UUID, GUI> openGUIs = new ConcurrentHashMap<>();

    /**
     * Registers a GUI as the player's currently-open GUI, replacing any
     * previous one.
     *
     * @param player the player
     * @param gui     the GUI being opened
     */
    public void register(Player player, GUI gui) {
        openGUIs.put(player.getUniqueId(), gui);
    }

    /**
     * Removes the player's open GUI entry (called on inventory close).
     *
     * @param player the player
     */
    public void unregister(Player player) {
        openGUIs.remove(player.getUniqueId());
    }

    /**
     * Returns the GUI the player currently has open, or {@code null}.
     *
     * @param player the player
     * @return the open GUI, or null
     */
    public GUI getGUI(Player player) {
        return openGUIs.get(player.getUniqueId());
    }

    /**
     * Returns whether the player currently has a plugin GUI open.
     *
     * @param player the player
     * @return true if a GUI is open
     */
    public boolean hasGUI(Player player) {
        return openGUIs.containsKey(player.getUniqueId());
    }

    /**
     * Closes all open GUIs (used on plugin disable).
     */
    public void closeAll() {
        openGUIs.clear();
    }
}
