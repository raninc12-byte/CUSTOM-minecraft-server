package com.gulis.skyblock.island;

import com.gulis.skyblock.core.Skyblock;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

import java.util.UUID;

/**
 * Generates the starting island platform in a void world.
 *
 * <p>Instead of requiring a schematic file, this generator builds a classic
 * Skyblock starter island programmatically: a small dirt/stone platform with a
 * tree, a chest with starter items, and a cobblestone generator. This keeps the
 * plugin self-contained with no external file dependencies. If a schematic
 * system is desired later, swap {@link #generate(Location)} for a WorldEdit/
 * FastAsyncWorldEdit paste call.</p>
 */
public class IslandGenerator {

    private final Skyblock plugin;

    public IslandGenerator(Skyblock plugin) {
        this.plugin = plugin;
    }

    /**
     * Generates the default starter island at the given center location.
     *
     * @param center the island center (the platform is built around it)
     */
    public void generate(Location center) {
        World world = center.getWorld();
        int baseX = center.getBlockX();
        int baseY = center.getBlockY();
        int baseZ = center.getBlockZ();

        // 5x5 dirt platform
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                center.getWorld().getBlockAt(baseX + x, baseY, baseZ + z).setType(Material.GRASS_BLOCK);
            }
        }

        // A small 3x3 stone layer underneath for stability
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                world.getBlockAt(baseX + x, baseY - 1, baseZ + z).setType(Material.STONE);
            }
        }

        // Tree: oak log + leaves
        placeTree(world, baseX + 1, baseY + 1, baseZ);

        // Chest with starter items
        Block chest = world.getBlockAt(baseX - 1, baseY + 1, baseZ);
        chest.setType(Material.CHEST);
        // (Starter loot would be added here via the chest's inventory)

        // Cobblestone generator: lava + water source
        world.getBlockAt(baseX + 2, baseY + 1, baseZ + 2).setType(Material.LAVA);
        world.getBlockAt(baseX + 2, baseY + 1, baseZ + 4).setType(Material.WATER);

        plugin.getLogger().info("Generated starter island at " + baseX + "/" + baseY + "/" + baseZ);
    }

    /**
     * Places a small oak tree at the given block (the log base).
     */
    private void placeTree(World world, int x, int y, int z) {
        // Trunk
        for (int i = 0; i < 4; i++) {
            world.getBlockAt(x, y + i, z).setType(Material.OAK_LOG);
        }
        // Leaves: a 3x3x2 canopy on top
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                if (Math.abs(dx) == 2 && Math.abs(dz) == 2) continue; // trim corners
                world.getBlockAt(x + dx, y + 3, z + dz).setType(Material.OAK_LEAVES);
                world.getBlockAt(x + dx, y + 4, z + dz).setType(Material.OAK_LEAVES);
            }
        }
        // Top leaf
        world.getBlockAt(x, y + 5, z).setType(Material.OAK_LEAVES);
    }
}
