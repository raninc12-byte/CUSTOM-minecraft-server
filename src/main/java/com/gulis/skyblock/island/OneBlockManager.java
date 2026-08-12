package com.gulis.skyblock.island;

import com.gulis.skyblock.core.Skyblock;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * Implements the OneBlock game mode, inspired by the OneBlock map
 * (https://www.planetminecraft.com/project/oneblock-2-0/).
 *
 * <p>Each player gets a single "magic block" in a void world. Mining it
 * produces a random block (which drops as an item) and the block regenerates
 * as a new random block. As the player mines more blocks, they progress
 * through phases that unlock rarer blocks and occasionally spawn mobs.</p>
 *
 * <p>The magic block is tracked per-player by location. The block is
 * indestructible except by its owner mining it normally.</p>
 */
public class OneBlockManager {

    private final Skyblock plugin;
    private final Random random = new Random();

    // Per-player OneBlock state: location of their magic block + blocks mined
    private final Map<UUID, OneBlockData> blocks = new HashMap<>();

    public OneBlockManager(Skyblock plugin) {
        this.plugin = plugin;
    }

    /**
     * Creates a new OneBlock for a player at the next free grid slot in the
     * island world. The player is teleported onto the block.
     *
     * @param player the player
     * @return true if created, false if the player already has one
     */
    public boolean createOneBlock(Player player) {
        UUID uuid = player.getUniqueId();
        if (blocks.containsKey(uuid)) {
            return false;
        }

        World world = plugin.getIslandManager().getIslandWorld();
        if (world == null) {
            plugin.getLogger().warning("Island world not loaded; cannot create OneBlock.");
            return false;
        }

        // Place OneBlocks on a separate grid offset from regular islands
        int index = blocks.size();
        int spacing = plugin.getConfigManager().getConfig().getInt("oneblock.grid-spacing", 512);
        int x = (index % 20) * spacing;
        int z = (index / 20) * spacing;
        int y = plugin.getConfigManager().getConfig().getInt("oneblock.start-y", 100);

        // Build a tiny 3x3 platform of dirt around the magic block so the
        // player doesn't immediately fall into the void.
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) continue;
                world.getBlockAt(x + dx, y - 1, z + dz).setType(Material.DIRT);
            }
        }

        // The magic block itself
        Block magic = world.getBlockAt(x, y, z);
        magic.setType(getBlockForPhase(0));

        OneBlockData data = new OneBlockData(magic.getLocation(), 0, 0);
        blocks.put(uuid, data);

        // Teleport the player onto the block
        player.teleport(new Location(world, x + 0.5, y + 1, z + 0.5));
        return true;
    }

    /**
     * Returns the OneBlock data for a player, or null.
     */
    public OneBlockData getOneBlock(UUID uuid) {
        return blocks.get(uuid);
    }

    /**
     * Called when a player mines a block. If it is their magic block, this
     * drops a random item, advances their counter, and regenerates the block.
     *
     * @param player the player who mined
     * @param block  the block that was broken
     * @return true if this was a OneBlock and was handled
     */
    public boolean handleBlockBreak(Player player, Block block) {
        OneBlockData data = blocks.get(player.getUniqueId());
        if (data == null) return false;
        if (!data.getLocation().equals(block.getLocation())) return false;

        // Drop the broken block as an item (so the player collects it)
        block.getWorld().dropItemNaturally(block.getLocation(),
                new ItemStack(block.getType(), 1));

        // Advance the mined counter and possibly the phase
        data.incrementMined();
        int newPhase = computePhase(data.getMined());
        if (newPhase != data.getPhase()) {
            data.setPhase(newPhase);
            player.sendMessage(ChatColor.GREEN + "OneBlock phase: "
                    + getPhaseName(newPhase) + ChatColor.GREEN + "!");
        }

        // Occasionally spawn a mob (10% chance)
        if (random.nextDouble() < 0.10) {
            spawnMob(block.getLocation());
        }

        // Regenerate the magic block as a new random block for the current phase
        block.setType(getBlockForPhase(data.getPhase()));
        return true;
    }

    /**
     * Computes the phase number from the total blocks mined.
     */
    private int computePhase(int mined) {
        // Each phase is 200 blocks. Phase 0 = 0-199, phase 1 = 200-399, etc.
        return mined / 200;
    }

    /**
     * Returns a random block material appropriate for the given phase.
     */
    private Material getBlockForPhase(int phase) {
        Material[] pool = BLOCK_POOLS[Math.min(phase, BLOCK_POOLS.length - 1)];
        return pool[random.nextInt(pool.length)];
    }

    /**
     * Returns a human-readable name for a phase.
     */
    public static String getPhaseName(int phase) {
        if (phase < PHASE_NAMES.length) return PHASE_NAMES[phase];
        return "Endgame";
    }

    /**
     * Spawns a random hostile mob near the block.
     */
    private void spawnMob(Location loc) {
        World world = loc.getWorld();
        if (world == null) return;
        org.bukkit.entity.EntityType[] mobs = {
                org.bukkit.entity.EntityType.ZOMBIE,
                org.bukkit.entity.EntityType.SKELETON,
                org.bukkit.entity.EntityType.SPIDER,
                org.bukkit.entity.EntityType.CREEPER
        };
        org.bukkit.entity.EntityType mob = mobs[random.nextInt(mobs.length)];
        world.spawnEntity(loc.clone().add(0.5, 1, 0.5), mob);
    }

    /**
     * Per-player OneBlock state.
     */
    public static class OneBlockData {
        private final Location location;
        private int mined;
        private int phase;

        public OneBlockData(Location location, int mined, int phase) {
            this.location = location;
            this.mined = mined;
            this.phase = phase;
        }

        public Location getLocation() { return location; }
        public int getMined() { return mined; }
        public int getPhase() { return phase; }
        public void setPhase(int phase) { this.phase = phase; }
        public void incrementMined() { mined++; }
    }

    // Block pools per phase. Phase 0 = basic, later phases = rarer blocks.
    private static final Material[][] BLOCK_POOLS = {
            // Phase 0: The Beginning
            {Material.STONE, Material.DIRT, Material.COBBLESTONE, Material.OAK_LOG,
                    Material.GRAVEL, Material.SAND, Material.COAL_ORE, Material.GRASS_BLOCK},
            // Phase 1: Underground
            {Material.IRON_ORE, Material.COAL_ORE, Material.STONE, Material.GOLD_ORE,
                    Material.REDSTONE_ORE, Material.LAPIS_ORE, Material.OAK_LOG, Material.CLAY},
            // Phase 2: Deep Slate
            {Material.DEEPSLATE, Material.DEEPSLATE_IRON_ORE, Material.DEEPSLATE_GOLD_ORE,
                    Material.DEEPSLATE_DIAMOND_ORE, Material.DEEPSLATE_EMERALD_ORE,
                    Material.DEEPSLATE_REDSTONE_ORE, Material.DEEPSLATE_COAL_ORE, Material.TUFF},
            // Phase 3: The Nether
            {Material.NETHERRACK, Material.NETHER_QUARTZ_ORE, Material.NETHER_GOLD_ORE,
                    Material.SOUL_SAND, Material.MAGMA_BLOCK, Material.OBSIDIAN,
                    Material.GLOWSTONE, Material.BLACKSTONE},
            // Phase 4: Ocean
            {Material.PRISMARINE, Material.SEA_LANTERN, Material.SPONGE, Material.WET_SPONGE,
                    Material.TUBE_CORAL_BLOCK, Material.SAND, Material.CLAY, Material.DARK_PRISMARINE},
            // Phase 5: Endgame
            {Material.END_STONE, Material.PURPUR_BLOCK, Material.END_STONE_BRICKS,
                    Material.OBSIDIAN, Material.ANCIENT_DEBRIS, Material.DIAMOND_BLOCK,
                    Material.EMERALD_BLOCK, Material.NETHERITE_BLOCK},
    };

    private static final String[] PHASE_NAMES = {
            "The Beginning", "Underground", "Deep Slate", "The Nether",
            "Ocean", "Endgame"
    };
}
