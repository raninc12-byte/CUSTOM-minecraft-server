package com.gulis.skyblock.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.InventoryHolder;

/**
 * Central listener that routes inventory events to the correct GUI.
 *
 * <p>Two mechanisms are used to identify a plugin-owned GUI:</p>
 * <ol>
 *   <li>The inventory's {@link InventoryHolder} is a {@link GUI} instance
 *       (set in {@link GUI#GUI}). This is the primary, reliable mechanism.</li>
 *   <li>The {@link GUIManager} tracks the player's open GUI as a fallback.</li>
 * </ol>
 *
 * <p>All clicks and drags inside a plugin GUI are cancelled to prevent item
 * theft, then forwarded to the GUI's own click handler.</p>
 */
public class GUIListener implements Listener {

    private final GUIManager guiManager;

    public GUIListener(GUIManager guiManager) {
        this.guiManager = guiManager;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        GUI gui = resolveGUI(event);

        if (gui == null) {
            return;
        }

        // Always cancel: players should not be able to take items out of a GUI
        event.setCancelled(true);

        // Ignore clicks in the player's own inventory section of the view
        if (event.getClickedInventory() == null
                || !event.getClickedInventory().equals(gui.getInventory())) {
            return;
        }

        gui.handleClick(event);
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        GUI gui = resolveGUI(event.getInventory().getHolder());
        if (gui != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getPlayer();
        // Only unregister if the closing inventory was actually our GUI
        GUI gui = guiManager.getGUI(player);
        if (gui != null && gui.getInventory().equals(event.getInventory())) {
            guiManager.unregister(player);
        }
    }

    /**
     * Resolves the GUI from a click event, preferring the holder mechanism.
     */
    private GUI resolveGUI(InventoryClickEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        return resolveGUI(holder);
    }

    /**
     * Resolves the GUI from an inventory holder. Falls back to the manager's
     * per-player tracking if the holder is not a GUI (e.g. nested views).
     */
    private GUI resolveGUI(InventoryHolder holder) {
        if (holder instanceof GUI) {
            return (GUI) holder;
        }
        return null;
    }
}
