package com.gulis.skyblock.island;

import com.gulis.skyblock.core.Skyblock;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

/**
 * Listens for block breaks and routes them to the {@link OneBlockManager} so
 * the magic block can regenerate and drop items.
 */
public class OneBlockListener implements Listener {

    private final Skyblock plugin;

    public OneBlockListener(Skyblock plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        // If this was a OneBlock, cancel the normal break (we handle it
        // ourselves so the block regenerates) and let the manager process it.
        if (plugin.getOneBlockManager().handleBlockBreak(event.getPlayer(), event.getBlock())) {
            event.setCancelled(true);
        }
    }
}
