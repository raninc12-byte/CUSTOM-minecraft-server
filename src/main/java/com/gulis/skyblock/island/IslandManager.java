package com.gulis.skyblock.island;

import com.gulis.skyblock.core.Skyblock;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.generator.ChunkGenerator;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * Manages island lifecycle: creation, loading, saving, deletion, and lookup.
 *
 * <p>Islands are spaced on a grid in a dedicated void world. Each new island
 * is placed at the next free grid slot. The grid spacing is configurable in
 * {@code config.yml}. Island data is cached in memory and persisted to SQLite.</p>
 */
public class IslandManager {

    private final Skyblock plugin;
    private final Map<UUID, Island> islandsByOwner = new HashMap<>();
    private final Map<Integer, Island> islandsById = new HashMap<>();
    private final IslandGenerator generator;

    private World islandWorld;
    private int gridSpacing;
    private int islandY;

    public IslandManager(Skyblock plugin) {
        this.plugin = plugin;
        this.generator = new IslandGenerator(plugin);
        this.gridSpacing = plugin.getConfigManager().getConfig().getInt("island.grid-spacing", 256);
        this.islandY = plugin.getConfigManager().getConfig().getInt("island.start-y", 100);
    }

    /**
     * Loads the void world and all existing islands from the database.
     */
    public void loadIslands() {
        setupWorld();
        try (var rs = plugin.getDatabaseManager().query(
                "SELECT id, owner_uuid, center_x, center_y, center_z, world, level, members FROM islands")) {
            while (rs.next()) {
                int id = rs.getInt("id");
                UUID owner = UUID.fromString(rs.getString("owner_uuid"));
                int cx = rs.getInt("center_x");
                int cy = rs.getInt("center_y");
                int cz = rs.getInt("center_z");
                String worldName = rs.getString("world");
                double level = rs.getDouble("level");
                String membersJson = rs.getString("members");

                World world = Bukkit.getWorld(worldName);
                if (world == null) world = islandWorld;
                Location center = new Location(world, cx, cy, cz);
                Island island = new Island(id, owner, center, level);

                // Parse members JSON array (simple format: ["uuid","uuid"])
                if (membersJson != null && membersJson.length() > 2) {
                    String inner = membersJson.substring(1, membersJson.length() - 1);
                    for (String s : inner.split(",")) {
                        s = s.replace("\"", "").trim();
                        if (!s.isEmpty()) {
                            island.addMember(UUID.fromString(s));
                        }
                    }
                }

                islandsByOwner.put(owner, island);
                islandsById.put(id, island);
            }
            plugin.getLogger().info("Loaded " + islandsByOwner.size() + " islands.");
        } catch (Exception e) {
            plugin.getLogger().severe("Could not load islands: " + e.getMessage());
        }
    }

    /**
     * Creates (or loads) the void world used for islands.
     */
    private void setupWorld() {
        String worldName = plugin.getConfigManager().getConfig().getString("island.world", "skyblock_world");
        islandWorld = Bukkit.getWorld(worldName);
        if (islandWorld == null) {
            WorldCreator creator = new WorldCreator(worldName);
            creator.environment(World.Environment.NORMAL);
            creator.type(WorldType.FLAT);
            creator.generator(new VoidGenerator());
            islandWorld = creator.createWorld();
            plugin.getLogger().info("Created island world: " + worldName);
        }
    }

    /**
     * Creates a new island for a player. The player must not already own one.
     *
     * @param ownerUuid the owner's UUID
     * @return the new island, or null if the player already has one
     */
    public Island createIsland(UUID ownerUuid) {
        if (islandsByOwner.containsKey(ownerUuid)) {
            return null; // already has an island
        }

        // Find the next free grid slot using a simple spiral/linear search
        Location center = findNextFreeSlot();

        // Generate the island terrain
        generator.generate(center);

        // Persist to database
        int id;
        try {
            id = plugin.getDatabaseManager().insertAndGetKey(
                    "INSERT INTO islands (owner_uuid, center_x, center_y, center_z, world, level, members) "
                            + "VALUES (?, ?, ?, ?, ?, ?, ?)",
                    ownerUuid.toString(),
                    center.getBlockX(),
                    center.getBlockY(),
                    center.getBlockZ(),
                    center.getWorld().getName(),
                    0.0,
                    "[]");
        } catch (Exception e) {
            plugin.getLogger().severe("Could not insert island: " + e.getMessage());
            return null;
        }

        Island island = new Island(id, ownerUuid, center, 0.0);
        islandsByOwner.put(ownerUuid, island);
        islandsById.put(id, island);
        return island;
    }

    /**
     * Finds the next free grid slot for a new island.
     */
    private Location findNextFreeSlot() {
        // Simple linear search along a spiral for the first free slot
        int index = islandsByOwner.size();
        // Arrange islands in a grid: index 0 at origin, then spiral outward
        int ring = (int) Math.floor((Math.sqrt(index + 1) - 1) / 2);
        int ringStart = (2 * ring - 1) * (2 * ring - 1);
        int posInRing = index - ringStart;
        int side = 2 * ring;
        int x, z;
        if (side == 0) {
            x = 0; z = 0;
        } else {
            int sideIdx = posInRing / side;
            int sidePos = posInRing % side;
            switch (sideIdx) {
                case 0: x = -ring + sidePos; z = -ring; break;
                case 1: x = ring; z = -ring + sidePos; break;
                case 2: x = ring - sidePos; z = ring; break;
                default: x = -ring; z = ring - sidePos; break;
            }
        }
        return new Location(islandWorld, x * gridSpacing, islandY, z * gridSpacing);
    }

    /**
     * Deletes an island: removes blocks (optional) and the database row.
     *
     * @param ownerUuid the owner's UUID
     * @return true if an island was deleted
     */
    public boolean deleteIsland(UUID ownerUuid) {
        Island island = islandsByOwner.remove(ownerUuid);
        if (island == null) return false;
        islandsById.remove(island.getId());
        try {
            plugin.getDatabaseManager().update("DELETE FROM islands WHERE owner_uuid = ?",
                    ownerUuid.toString());
        } catch (Exception e) {
            plugin.getLogger().severe("Could not delete island: " + e.getMessage());
        }
        return true;
    }

    public Island getIsland(UUID ownerUuid) {
        return islandsByOwner.get(ownerUuid);
    }

    public Island getIslandById(int id) {
        return islandsById.get(id);
    }

    /**
     * Saves all dirty islands to the database.
     */
    public void saveIslands() {
        for (Island island : islandsByOwner.values()) {
            saveIsland(island);
        }
    }

    /**
     * Persists a single island's mutable fields (level, members).
     */
    public void saveIsland(Island island) {
        StringBuilder membersJson = new StringBuilder("[");
        for (int i = 0; i < island.getMembers().size(); i++) {
            if (i > 0) membersJson.append(",");
            membersJson.append("\"").append(island.getMembers().get(i).toString()).append("\"");
        }
        membersJson.append("]");
        try {
            plugin.getDatabaseManager().update(
                    "UPDATE islands SET level = ?, members = ? WHERE id = ?",
                    island.getLevel(), membersJson.toString(), island.getId());
        } catch (Exception e) {
            plugin.getLogger().warning("Could not save island " + island.getId() + ": " + e.getMessage());
        }
    }

    public World getIslandWorld() {
        return islandWorld;
    }

    public Map<UUID, Island> getAllIslands() {
        return islandsByOwner;
    }

    public IslandGenerator getGenerator() {
        return generator;
    }

    /**
     * Custom chunk generator that produces an empty (void) world.
     */
    public static class VoidGenerator extends ChunkGenerator {
        @Override
        public ChunkData generateChunkData(World world, Random random, int chunkX, int chunkZ, BiomeGrid biome) {
            return createChunkData(world);
        }
    }
}
