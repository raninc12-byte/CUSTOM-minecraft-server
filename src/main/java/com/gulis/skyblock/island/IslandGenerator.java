package com.gulis.skyblock.island;

import com.gulis.skyblock.core.Skyblock;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.inventory.ItemStack;

import java.util.Random;

/**
 * Generates Trog's-Skyblock-style starter islands (v4+ accurate).
 *
 * Based on Trog's Skyblock (https://www.planetminecraft.com/project/trog-skyblock/):
 * - Main island: 15x15 grass platform with biome sections, tree, cobble gen, chest
 * - 8 surrounding resource islands at 30 blocks: sand, gravel, clay, soul sand,
 *   obsidian, ice, mycelium, mushroom
 * - Nether island at 50 blocks with netherrack, quartz, gold, blaze spawner area
 * - End island at 70 blocks with end stone, purpur, chorus
 * - Floating woodland mansion at 100 blocks
 * - Floating ocean monument at -100 blocks
 * - Witch hut, buried treasure, shipwreck, ruined portal scattered around
 */
public class IslandGenerator {

    private final Skyblock plugin;
    private final Random random = new Random();

    public IslandGenerator(Skyblock plugin) {
        this.plugin = plugin;
    }

    /**
     * Generates a lightweight starter island immediately.
     * Other structures (resource islands, nether/end islands, floating structures)
     * are generated on-demand when the player visits them via /is visit commands.
     *
     * @param center the main island center
     */
    public void generate(Location center) {
        World world = center.getWorld();
        int cx = center.getBlockX();
        int cy = center.getBlockY();
        int cz = center.getBlockZ();

        // Only generate the main island immediately - this is lightweight
        buildMainIsland(world, cx, cy, cz);
        
        // Store the island center for on-demand generation of other structures
        // The IslandManager will handle on-demand generation when player uses /is visit
        
        plugin.getLogger().info("Generated lightweight starter island at " + cx + "/" + cy + "/" + cz);
    }

    /**
     * Generates the 8 surrounding resource islands on-demand.
     * Called when player uses /is visit resource
     */
    public void generateResourceIslands(Location center) {
        World world = center.getWorld();
        int cx = center.getBlockX();
        int cy = center.getBlockY();
        int cz = center.getBlockZ();
        
        Bukkit.getScheduler().runTask(plugin, () -> {
            buildSurroundingIslands(world, cx, cy, cz);
            plugin.getLogger().info("Generated resource islands on-demand");
        });
    }

    /**
     * Generates the nether island on-demand.
     */
    public void generateNetherIsland(Location center) {
        World world = center.getWorld();
        int cx = center.getBlockX();
        int cy = center.getBlockY();
        int cz = center.getBlockZ();
        
        Bukkit.getScheduler().runTask(plugin, () -> {
            buildNetherIsland(world, cx + 50, cy, cz);
            plugin.getLogger().info("Generated nether island on-demand");
        });
    }

    /**
     * Generates the end island on-demand.
     */
    public void generateEndIsland(Location center) {
        World world = center.getWorld();
        int cx = center.getBlockX();
        int cy = center.getBlockY();
        int cz = center.getBlockZ();
        
        Bukkit.getScheduler().runTask(plugin, () -> {
            buildEndIsland(world, cx - 70, cy + 10, cz + 70);
            plugin.getLogger().info("Generated end island on-demand");
        });
    }

    /**
     * Generates floating structures on-demand.
     */
    public void generateFloatingStructures(Location center) {
        World world = center.getWorld();
        int cx = center.getBlockX();
        int cy = center.getBlockY();
        int cz = center.getBlockZ();
        
        Bukkit.getScheduler().runTask(plugin, () -> {
            buildFloatingMansion(world, cx + 100, cy + 25, cz + 100);
            buildFloatingOceanMonument(world, cx - 100, cy + 20, cz - 100);
            plugin.getLogger().info("Generated floating structures on-demand");
        });
    }

    /**
     * Generates scattered structures on-demand.
     */
    public void generateScatteredStructures(Location center) {
        World world = center.getWorld();
        int cx = center.getBlockX();
        int cy = center.getBlockY();
        int cz = center.getBlockZ();
        
        Bukkit.getScheduler().runTask(plugin, () -> {
            buildWitchHut(world, cx + 40, cy, cz - 30);
            buildBuriedTreasure(world, cx - 35, cy, cz + 45);
            buildRuinedPortal(world, cx + 60, cy, cz - 60);
            plugin.getLogger().info("Generated scattered structures on-demand");
        });
    }

    /**
     * Builds the main starter island: 15x15 grass platform with biome sections,
     * tree, proper cobblestone generator, and starter chest.
     */
    private void buildMainIsland(World world, int cx, int cy, int cz) {
        // 15x15 circular platform
        for (int x = -7; x <= 7; x++) {
            for (int z = -7; z <= 7; z++) {
                double dist = Math.sqrt(x * x + z * z);
                if (dist <= 7.5) {
                    // Biome variation: center=grass, edges=dirt/sand
                    Material surface = dist <= 3 ? Material.GRASS_BLOCK :
                                     dist <= 5 ? Material.DIRT : Material.SAND;
                    world.getBlockAt(cx + x, cy, cz + z).setType(surface);
                    // 3 layers of dirt/stone below
                    for (int y = 1; y <= 3; y++) {
                        world.getBlockAt(cx + x, cy - y, cz + z).setType(y == 3 ? Material.STONE : Material.DIRT);
                    }
                }
            }
        }

        // Large oak tree (offset)
        placeLargeTree(world, cx + 3, cy + 1, cz - 3);

        // Starter chest with Trog's Skyblock items
        Block chest = world.getBlockAt(cx - 3, cy + 1, cz);
        chest.setType(Material.CHEST);
        if (chest.getState() instanceof Chest chestState) {
            chestState.getBlockInventory().addItem(
                    new ItemStack(Material.WATER_BUCKET, 1),
                    new ItemStack(Material.LAVA_BUCKET, 1),
                    new ItemStack(Material.ICE, 2),
                    new ItemStack(Material.BONE_MEAL, 32),
                    new ItemStack(Material.OAK_SAPLING, 4),
                    new ItemStack(Material.SUGAR_CANE, 5),
                    new ItemStack(Material.CACTUS, 2),
                    new ItemStack(Material.MELON_SLICE, 4),
                    new ItemStack(Material.PUMPKIN_SEEDS, 4),
                    new ItemStack(Material.BEETROOT_SEEDS, 4)
            );
        }

        // PROPER cobblestone generator (classic 4-block design)
        buildCobbleGenerator(world, cx + 5, cy + 1, cz + 5);

        // Small pond for fishing
        buildPond(world, cx - 5, cy, cz - 5);
    }

    /**
     * Builds a proper cobblestone generator:
     *  Lava -> Cobble <- Water
     *  With non-flammable blocks around
     */
    private void buildCobbleGenerator(World world, int x, int y, int z) {
        // Base platform
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                world.getBlockAt(x + dx, y - 1, z + dz).setType(Material.COBBLESTONE);
            }
        }
        // Generator: lava at (0,0), water at (2,0), cobble forms at (1,0)
        world.getBlockAt(x, y, z).setType(Material.LAVA);
        world.getBlockAt(x + 2, y, z).setType(Material.WATER);
        world.getBlockAt(x + 1, y, z).setType(Material.COBBLESTONE);
        // Non-flammable walls
        world.getBlockAt(x - 1, y, z).setType(Material.COBBLESTONE);
        world.getBlockAt(x + 3, y, z).setType(Material.COBBLESTONE);
        world.getBlockAt(x + 1, y, z - 1).setType(Material.COBBLESTONE);
        world.getBlockAt(x + 1, y, z + 1).setType(Material.COBBLESTONE);
    }

    /**
     * Builds a small fishing pond.
     */
    private void buildPond(World world, int x, int y, int z) {
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                double dist = Math.sqrt(dx * dx + dz * dz);
                if (dist <= 2.5) {
                    world.getBlockAt(x + dx, y, z + dz).setType(Material.WATER);
                    world.getBlockAt(x + dx, y - 1, z + dz).setType(Material.SAND);
                }
            }
        }
        // Lily pads
        world.getBlockAt(x, y + 1, z).setType(Material.LILY_PAD);
        world.getBlockAt(x + 1, y + 1, z + 1).setType(Material.LILY_PAD);
    }

    /**
     * Places a larger oak tree (5 tall trunk, bigger canopy).
     */
    private void placeLargeTree(World world, int x, int y, int z) {
        // Trunk (5 tall)
        for (int i = 0; i < 5; i++) {
            world.getBlockAt(x, y + i, z).setType(Material.OAK_LOG);
        }
        // Canopy layers
        for (int layer = 0; layer < 3; layer++) {
            int radius = 3 - layer;
            int ly = y + 4 + layer;
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (dx * dx + dz * dz <= radius * radius + 1) {
                        world.getBlockAt(x + dx, ly, z + dz).setType(Material.OAK_LEAVES);
                    }
                }
            }
        }
        // Top
        world.getBlockAt(x, y + 7, z).setType(Material.OAK_LEAVES);
    }

    /**
     * Builds the eight surrounding resource islands at 30-block radius.
     */
    private void buildSurroundingIslands(World world, int cx, int cy, int cz) {
        int radius = 30;
        // {dx, dz, surface, sub-surface, name}
        Object[][] islands = {
                {1, 0, Material.SAND, Material.SANDSTONE, "Sand Island"},
                {-1, 0, Material.GRAVEL, Material.COBBLESTONE, "Gravel Island"},
                {0, 1, Material.CLAY, Material.CLAY, "Clay Island"},
                {0, -1, Material.SOUL_SAND, Material.SOUL_SOIL, "Soul Sand Island"},
                {1, 1, Material.OBSIDIAN, Material.OBSIDIAN, "Obsidian Island"},
                {-1, -1, Material.PACKED_ICE, Material.BLUE_ICE, "Ice Island"},
                {1, -1, Material.MYCELIUM, Material.PODZOL, "Mushroom Island"},
                {-1, 1, Material.RED_SAND, Material.RED_SANDSTONE, "Red Sand Island"},
        };
        for (Object[] island : islands) {
            int dx = (int) island[0] * radius;
            int dz = (int) island[1] * radius;
            Material surface = (Material) island[2];
            Material sub = (Material) island[3];
            buildResourceIsland(world, cx + dx, cy, cz + dz, surface, sub, 5);
        }
    }

    /**
     * Builds a resource island (5x5 platform, 3 layers deep).
     */
    private void buildResourceIsland(World world, int x, int y, int z, Material surface, Material sub, int size) {
        int half = size / 2;
        for (int dx = -half; dx <= half; dx++) {
            for (int dz = -half; dz <= half; dz++) {
                double dist = Math.sqrt(dx * dx + dz * dz);
                if (dist <= half + 0.5) {
                    world.getBlockAt(x + dx, y, z + dz).setType(surface);
                    for (int ly = 1; ly <= 3; ly++) {
                        world.getBlockAt(x + dx, y - ly, z + dz).setType(ly == 3 ? Material.STONE : sub);
                    }
                }
            }
        }
        // Add a landmark block on top center
        world.getBlockAt(x, y + 1, z).setType(Material.GLOWSTONE);
    }

    /**
     * Builds a Nether-themed island with netherrack, quartz, gold, and a blaze spawner platform.
     */
    private void buildNetherIsland(World world, int x, int y, int z) {
        // 9x9 platform
        for (int dx = -4; dx <= 4; dx++) {
            for (int dz = -4; dz <= 4; dz++) {
                double dist = Math.sqrt(dx * dx + dz * dz);
                if (dist <= 4.5) {
                    world.getBlockAt(x + dx, y, z + dz).setType(Material.NETHERRACK);
                    for (int ly = 1; ly <= 3; ly++) {
                        world.getBlockAt(x + dx, y - ly, z + dz).setType(ly == 3 ? Material.BLACKSTONE : Material.NETHERRACK);
                    }
                }
            }
        }
        // Quartz patches
        world.getBlockAt(x + 2, y + 1, z).setType(Material.QUARTZ_BLOCK);
        world.getBlockAt(x - 2, y + 1, z).setType(Material.QUARTZ_BLOCK);
        world.getBlockAt(x, y + 1, z + 2).setType(Material.QUARTZ_BLOCK);
        world.getBlockAt(x, y + 1, z - 2).setType(Material.QUARTZ_BLOCK);
        // Gold ore
        world.getBlockAt(x + 1, y - 1, z + 1).setType(Material.NETHER_GOLD_ORE);
        world.getBlockAt(x - 1, y - 1, z - 1).setType(Material.NETHER_GOLD_ORE);
        // Blaze spawner platform (surrounded by nether brick fence)
        world.getBlockAt(x, y + 3, z).setType(Material.SPAWNER);
        // Note: Spawner type would need NBT to set to BLAZE
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx != 0 || dz != 0) {
                    world.getBlockAt(x + dx, y + 3, z + dz).setType(Material.NETHER_BRICK_FENCE);
                }
            }
        }
        // Glowstone lighting
        world.getBlockAt(x, y + 4, z).setType(Material.GLOWSTONE);
    }

    /**
     * Builds an End-themed island with end stone, purpur, and chorus plants.
     */
    private void buildEndIsland(World world, int x, int y, int z) {
        // 7x7 platform
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                double dist = Math.sqrt(dx * dx + dz * dz);
                if (dist <= 3.5) {
                    world.getBlockAt(x + dx, y, z + dz).setType(Material.END_STONE);
                    for (int ly = 1; ly <= 3; ly++) {
                        world.getBlockAt(x + dx, y - ly, z + dz).setType(Material.END_STONE);
                    }
                }
            }
        }
        // Purpur pillars
        for (int dy = 1; dy <= 5; dy++) {
            world.getBlockAt(x, y + dy, z).setType(Material.PURPUR_BLOCK);
        }
        // Chorus plants around
        for (int i = 0; i < 4; i++) {
            int dx = (i % 2 == 0) ? 2 : -2;
            int dz = (i < 2) ? 2 : -2;
            world.getBlockAt(x + dx, y + 1, z + dz).setType(Material.CHORUS_PLANT);
            world.getBlockAt(x + dx, y + 2, z + dz).setType(Material.CHORUS_FLOWER);
        }
        // End gateway (bedrock)
        world.getBlockAt(x + 3, y + 1, z).setType(Material.BEDROCK);
    }

    /**
     * Builds a floating woodland mansion (larger, more detailed).
     */
    private void buildFloatingMansion(World world, int x, int y, int z) {
        // 11x11 dark oak floor
        for (int dx = 0; dx < 11; dx++) {
            for (int dz = 0; dz < 11; dz++) {
                world.getBlockAt(x + dx, y, z + dz).setType(Material.DARK_OAK_PLANKS);
            }
        }
        // Walls (3 tall)
        for (int dy = 1; dy <= 3; dy++) {
            for (int dx = 0; dx < 11; dx++) {
                world.getBlockAt(x + dx, y + dy, z).setType(Material.DARK_OAK_LOG);
                world.getBlockAt(x + dx, y + dy, z + 10).setType(Material.DARK_OAK_LOG);
            }
            for (int dz = 0; dz < 11; dz++) {
                world.getBlockAt(x, y + dy, z + dz).setType(Material.DARK_OAK_LOG);
                world.getBlockAt(x + 10, y + dy, z + dz).setType(Material.DARK_OAK_LOG);
            }
        }
        // Windows
        world.getBlockAt(x + 3, y + 1, z).setType(Material.BLACK_STAINED_GLASS_PANE);
        world.getBlockAt(x + 7, y + 1, z).setType(Material.BLACK_STAINED_GLASS_PANE);
        world.getBlockAt(x + 3, y + 1, z + 10).setType(Material.BLACK_STAINED_GLASS_PANE);
        world.getBlockAt(x + 7, y + 1, z + 10).setType(Material.BLACK_STAINED_GLASS_PANE);
        // Roof
        for (int dx = 0; dx < 11; dx++) {
            for (int dz = 0; dz < 11; dz++) {
                world.getBlockAt(x + dx, y + 4, z + dz).setType(Material.DARK_OAK_SLAB);
            }
        }
        // Loot chests (2)
        Block chest1 = world.getBlockAt(x + 3, y + 1, z + 3);
        chest1.setType(Material.CHEST);
        if (chest1.getState() instanceof Chest c) {
            c.getBlockInventory().addItem(
                    new ItemStack(Material.EMERALD, 16),
                    new ItemStack(Material.TOTEM_OF_UNDYING, 2),
                    new ItemStack(Material.DIAMOND, 6),
                    new ItemStack(Material.ENCHANTED_GOLDEN_APPLE, 2),
                    new ItemStack(Material.DIAMOND_SWORD, 1)
            );
        }
        Block chest2 = world.getBlockAt(x + 7, y + 1, z + 7);
        chest2.setType(Material.CHEST);
        if (chest2.getState() instanceof Chest c) {
            c.getBlockInventory().addItem(
                    new ItemStack(Material.ELYTRA, 1),
                    new ItemStack(Material.SHULKER_SHELL, 4),
                    new ItemStack(Material.DIAMOND_BLOCK, 2)
            );
        }
        // Lanterns
        world.getBlockAt(x + 5, y + 5, z + 5).setType(Material.SEA_LANTERN);
    }

    /**
     * Builds a floating ocean monument (prismarine, guardians, sponge room).
     */
    private void buildFloatingOceanMonument(World world, int x, int y, int z) {
        // 15x15 prismarine base
        for (int dx = 0; dx < 15; dx++) {
            for (int dz = 0; dz < 15; dz++) {
                world.getBlockAt(x + dx, y, z + dz).setType(Material.PRISMARINE);
            }
        }
        // Walls (4 tall)
        for (int dy = 1; dy <= 4; dy++) {
            for (int dx = 0; dx < 15; dx++) {
                world.getBlockAt(x + dx, y + dy, z).setType(Material.PRISMARINE_BRICKS);
                world.getBlockAt(x + dx, y + dy, z + 14).setType(Material.PRISMARINE_BRICKS);
            }
            for (int dz = 0; dz < 15; dz++) {
                world.getBlockAt(x, y + dy, z + dz).setType(Material.PRISMARINE_BRICKS);
                world.getBlockAt(x + 14, y + dy, z + dz).setType(Material.PRISMARINE_BRICKS);
            }
        }
        // Sea lanterns at corners and center
        int[][] lanterns = {{0,0},{14,0},{0,14},{14,14},{7,7}};
        for (int[] l : lanterns) {
            world.getBlockAt(x + l[0], y + 1, z + l[1]).setType(Material.SEA_LANTERN);
        }
        // Sponge room (center)
        for (int dx = 5; dx <= 9; dx++) {
            for (int dz = 5; dz <= 9; dz++) {
                world.getBlockAt(x + dx, y + 1, z + dz).setType(Material.SPONGE);
                world.getBlockAt(x + dx, y + 2, z + dz).setType(Material.WET_SPONGE);
            }
        }
        // Gold blocks (treasure)
        world.getBlockAt(x + 7, y + 1, z + 7).setType(Material.GOLD_BLOCK);
        world.getBlockAt(x + 6, y + 1, z + 7).setType(Material.GOLD_BLOCK);
        world.getBlockAt(x + 7, y + 1, z + 6).setType(Material.GOLD_BLOCK);
        world.getBlockAt(x + 7, y + 1, z + 8).setType(Material.GOLD_BLOCK);
        world.getBlockAt(x + 8, y + 1, z + 7).setType(Material.GOLD_BLOCK);
    }

    /**
     * Builds a witch hut on stilts.
     */
    private void buildWitchHut(World world, int x, int y, int z) {
        // Stilts
        for (int dy = 0; dy < 4; dy++) {
            world.getBlockAt(x, y + dy, z).setType(Material.SPRUCE_LOG);
            world.getBlockAt(x + 4, y + dy, z).setType(Material.SPRUCE_LOG);
            world.getBlockAt(x, y + dy, z + 4).setType(Material.SPRUCE_LOG);
            world.getBlockAt(x + 4, y + dy, z + 4).setType(Material.SPRUCE_LOG);
        }
        // Floor
        for (int dx = 0; dx <= 4; dx++) {
            for (int dz = 0; dz <= 4; dz++) {
                world.getBlockAt(x + dx, y + 4, z + dz).setType(Material.SPRUCE_PLANKS);
            }
        }
        // Walls + roof (simplified)
        world.getBlockAt(x + 2, y + 5, z + 2).setType(Material.CAULDRON);
        world.getBlockAt(x + 2, y + 5, z + 3).setType(Material.BREWING_STAND);
        world.getBlockAt(x + 1, y + 5, z + 2).setType(Material.CRAFTING_TABLE);
        // Witch spawner
        world.getBlockAt(x + 2, y + 6, z + 2).setType(Material.SPAWNER);
    }

    /**
     * Builds a buried treasure chest.
     */
    private void buildBuriedTreasure(World world, int x, int y, int z) {
        // Sand covering
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                world.getBlockAt(x + dx, y + 1, z + dz).setType(Material.SAND);
            }
        }
        // Chest buried 2 blocks down
        Block chest = world.getBlockAt(x, y - 1, z);
        chest.setType(Material.CHEST);
        if (chest.getState() instanceof Chest c) {
            c.getBlockInventory().addItem(
                    new ItemStack(Material.HEART_OF_THE_SEA, 1),
                    new ItemStack(Material.DIAMOND, 4),
                    new ItemStack(Material.EMERALD, 8),
                    new ItemStack(Material.GOLD_INGOT, 16),
                    new ItemStack(Material.IRON_INGOT, 32),
                    new ItemStack(Material.TNT, 4)
            );
        }
    }

    /**
     * Builds a ruined portal.
     */
    private void buildRuinedPortal(World world, int x, int y, int z) {
        // Obsidian frame (partial)
        int[][] frame = {
            {0,0},{1,0},{2,0},{3,0},
            {0,1},        {3,1},
            {0,2},        {3,2},
            {0,3},{1,3},{2,3},{3,3}
        };
        for (int[] pos : frame) {
            if (random.nextDouble() < 0.7) { // 70% chance each block exists (ruined)
                world.getBlockAt(x + pos[0], y + pos[1], z).setType(Material.OBSIDIAN);
            }
        }
        // Crying obsidian scattered
        world.getBlockAt(x + 1, y + 1, z).setType(Material.CRYING_OBSIDIAN);
        world.getBlockAt(x + 2, y + 2, z).setType(Material.CRYING_OBSIDIAN);
        // Loot chest
        Block chest = world.getBlockAt(x + 1, y + 1, z - 1);
        chest.setType(Material.CHEST);
        if (chest.getState() instanceof Chest c) {
            c.getBlockInventory().addItem(
                    new ItemStack(Material.FLINT_AND_STEEL, 1),
                    new ItemStack(Material.OBSIDIAN, 8),
                    new ItemStack(Material.GOLDEN_APPLE, 2),
                    new ItemStack(Material.FIRE_CHARGE, 4)
            );
        }
        // Magma blocks around
        world.getBlockAt(x, y, z - 1).setType(Material.MAGMA_BLOCK);
        world.getBlockAt(x + 3, y, z - 1).setType(Material.MAGMA_BLOCK);
    }
}
