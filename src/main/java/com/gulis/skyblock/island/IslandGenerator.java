package com.gulis.skyblock.island;

import com.gulis.skyblock.core.Skyblock;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.inventory.ItemStack;

import java.util.Random;

/**
 * Generates Trog's-Skyblock-style starter islands.
 *
 * <p>Instead of requiring an external schematic file, this generator builds
 * the classic Skyblock layout programmatically, inspired by Trog's Skyblock
 * (https://www.planetminecraft.com/project/trog-skyblock/):</p>
 * <ul>
 *   <li>A central main island with grass, a tree, a chest of starter items,
 *       and a cobblestone generator.</li>
 *   <li>Eight smaller surrounding islands at a distance, each with a single
 *       resource (sand, gravel, clay, soul sand, obsidian, ice, mycelium,
 *       packed ice) — the classic Skyblock resource islands.</li>
 *   <li>Two floating structures far away: a mini woodland mansion and a
 *       sea temple, mirroring Trog's "floating structures" concept.</li>
 * </ul>
 *
 * <p>If a real schematic system is desired later, swap {@link #generate(Location)}
 * for a WorldEdit/FastAsyncWorldEdit paste call.</p>
 */
public class IslandGenerator {

    private final Skyblock plugin;
    private final Random random = new Random();

    public IslandGenerator(Skyblock plugin) {
        this.plugin = plugin;
    }

    /**
     * Generates the full Trog-style island cluster at the given center.
     *
     * @param center the main island center
     */
    public void generate(Location center) {
        World world = center.getWorld();
        int cx = center.getBlockX();
        int cy = center.getBlockY();
        int cz = center.getBlockZ();

        // 1. Main island
        buildMainIsland(world, cx, cy, cz);

        // 2. Eight surrounding resource islands (radius ~24 blocks)
        buildSurroundingIslands(world, cx, cy, cz);

        // 3. Two floating structures far away (~80 blocks)
        buildFloatingMansion(world, cx + 80, cy + 20, cz + 80);
        buildFloatingSeaTemple(world, cx - 80, cy + 15, cz - 80);

        plugin.getLogger().info("Generated Trog-style island cluster at " + cx + "/" + cy + "/" + cz);
    }

    /**
     * Builds the main starter island: a grass platform with a tree, a chest
     * of starter items, and a cobblestone generator.
     */
    private void buildMainIsland(World world, int cx, int cy, int cz) {
        // 7x7 grass platform, circular
        for (int x = -3; x <= 3; x++) {
            for (int z = -3; z <= 3; z++) {
                double dist = Math.sqrt(x * x + z * z);
                if (dist <= 3.5) {
                    world.getBlockAt(cx + x, cy, cz + z).setType(Material.GRASS_BLOCK);
                    // Dirt below
                    for (int y = 1; y <= 2; y++) {
                        world.getBlockAt(cx + x, cy - y, cz + z).setType(Material.DIRT);
                    }
                }
            }
        }

        // Tree: oak log + leaves (offset to one corner)
        placeTree(world, cx + 2, cy + 1, cz - 2);

        // Chest with starter items
        Block chest = world.getBlockAt(cx - 2, cy + 1, cz);
        chest.setType(Material.CHEST);
        if (chest.getState() instanceof Chest chestState) {
            chestState.getBlockInventory().addItem(
                    new ItemStack(Material.WATER_BUCKET, 1),
                    new ItemStack(Material.LAVA_BUCKET, 1),
                    new ItemStack(Material.ICE, 2),
                    new ItemStack(Material.BONE_MEAL, 16),
                    new ItemStack(Material.OAK_SAPLING, 4),
                    new ItemStack(Material.SUGAR_CANE, 3)
            );
        }

        // Cobblestone generator: lava + water source adjacent to each other
        world.getBlockAt(cx + 3, cy + 1, cz + 3).setType(Material.COBBLESTONE);
        world.getBlockAt(cx + 4, cy + 1, cz + 3).setType(Material.LAVA);
        world.getBlockAt(cx + 3, cy + 1, cz + 4).setType(Material.WATER);
    }

    /**
     * Builds the eight surrounding resource islands, each carrying one
     * scarce Skyblock resource.
     */
    private void buildSurroundingIslands(World world, int cx, int cy, int cz) {
        int radius = 24;
        // Each entry: {dx, dz, surface material, dirt material}
        Object[][] islands = {
                {1, 0, Material.SAND, Material.SAND},
                {-1, 0, Material.GRAVEL, Material.GRAVEL},
                {0, 1, Material.CLAY, Material.CLAY},
                {0, -1, Material.SOUL_SAND, Material.SOUL_SAND},
                {1, 1, Material.OBSIDIAN, Material.OBSIDIAN},
                {-1, -1, Material.BLUE_ICE, Material.PACKED_ICE},
                {1, -1, Material.MYCELIUM, Material.DIRT},
                {-1, 1, Material.MOSS_BLOCK, Material.DIRT},
        };
        for (Object[] island : islands) {
            int dx = (int) island[0] * radius;
            int dz = (int) island[1] * radius;
            Material surface = (Material) island[2];
            Material dirt = (Material) island[3];
            buildResourceIsland(world, cx + dx, cy, cz + dz, surface, dirt);
        }
    }

    /**
     * Builds a small 3x3 floating island of the given surface material.
     */
    private void buildResourceIsland(World world, int x, int y, int z, Material surface, Material dirt) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                world.getBlockAt(x + dx, y, z + dz).setType(surface);
                world.getBlockAt(x + dx, y - 1, z + dz).setType(dirt);
                world.getBlockAt(x + dx, y - 2, z + dz).setType(dirt);
            }
        }
    }

    /**
     * Builds a small floating "mansion" structure: a dark-oak box with a
     * chest of loot, representing the floating mansion from Trog's Skyblock.
     */
    private void buildFloatingMansion(World world, int x, int y, int z) {
        // 5x5 floor of dark oak planks
        for (int dx = 0; dx < 5; dx++) {
            for (int dz = 0; dz < 5; dz++) {
                world.getBlockAt(x + dx, y, z + dz).setType(Material.DARK_OAK_PLANKS);
            }
        }
        // 4 corner pillars (4 tall)
        int[][] corners = {{0, 0}, {4, 0}, {0, 4}, {4, 4}};
        for (int[] c : corners) {
            for (int dy = 1; dy <= 4; dy++) {
                world.getBlockAt(x + c[0], y + dy, z + c[1]).setType(Material.DARK_OAK_LOG);
            }
        }
        // Roof
        for (int dx = 0; dx < 5; dx++) {
            for (int dz = 0; dz < 5; dz++) {
                world.getBlockAt(x + dx, y + 5, z + dz).setType(Material.DARK_OAK_SLAB);
            }
        }
        // Loot chest in the center
        Block chest = world.getBlockAt(x + 2, y + 1, z + 2);
        chest.setType(Material.CHEST);
        if (chest.getState() instanceof Chest chestState) {
            chestState.getBlockInventory().addItem(
                    new ItemStack(Material.EMERALD, 8),
                    new ItemStack(Material.TOTEM_OF_UNDYING, 1),
                    new ItemStack(Material.DIAMOND, 3),
                    new ItemStack(Material.GOLDEN_APPLE, 2)
            );
        }
    }

    /**
     * Builds a small floating "sea temple": a prismarine platform with a
     * sponge and sea lanterns, representing the floating sea temple.
     */
    private void buildFloatingSeaTemple(World world, int x, int y, int z) {
        // 5x5 prismarine floor
        for (int dx = 0; dx < 5; dx++) {
            for (int dz = 0; dz < 5; dz++) {
                world.getBlockAt(x + dx, y, z + dz).setType(Material.PRISMARINE);
            }
        }
        // Sea lanterns at the corners
        int[][] corners = {{0, 0}, {4, 0}, {0, 4}, {4, 4}};
        for (int[] c : corners) {
            world.getBlockAt(x + c[0], y + 1, z + c[1]).setType(Material.SEA_LANTERN);
        }
        // Sponge in the center
        world.getBlockAt(x + 2, y + 1, z + 2).setType(Material.SPONGE);
        // Wet sponge on top
        world.getBlockAt(x + 2, y + 2, z + 2).setType(Material.WET_SPONGE);
    }

    /**
     * Places a small oak tree at the given block (the log base).
     */
    private void placeTree(World world, int x, int y, int z) {
        // Trunk (4 tall)
        for (int i = 0; i < 4; i++) {
            world.getBlockAt(x, y + i, z).setType(Material.OAK_LOG);
        }
        // Leaves: 5x5x2 canopy on top
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
