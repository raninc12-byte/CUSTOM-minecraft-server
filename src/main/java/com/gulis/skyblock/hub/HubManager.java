package com.gulis.skyblock.hub;

import com.gulis.skyblock.core.Skyblock;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.generator.ChunkGenerator;

import java.util.Random;

/**
 * Manages the hub world where players spawn when they join the server.
 *
 * <p>The hub is a void world with a central spawn platform. From here players
 * can open the main menu (via the item they receive, or {@code /menu}) to
 * choose a game mode: classic Skyblock or OneBlock. The hub keeps players
 * together before they venture off to their own islands.</p>
 */
public class HubManager {

    private final Skyblock plugin;
    private World hubWorld;
    private Location spawnLocation;

    public HubManager(Skyblock plugin) {
        this.plugin = plugin;
    }

    /**
     * Loads (or creates) the hub world and builds the spawn platform if it
     * does not yet exist.
     */
    public void loadHub() {
        String worldName = plugin.getConfigManager().getConfig()
                .getString("hub.world", "hub");
        hubWorld = Bukkit.getWorld(worldName);
        if (hubWorld == null) {
            WorldCreator creator = new WorldCreator(worldName);
            creator.environment(World.Environment.NORMAL);
            creator.type(WorldType.FLAT);
            creator.generator(new VoidGenerator());
            hubWorld = creator.createWorld();
            plugin.getLogger().info("Created hub world: " + worldName);
        }

        int spawnX = plugin.getConfigManager().getConfig().getInt("hub.spawn.x", 0);
        int spawnY = plugin.getConfigManager().getConfig().getInt("hub.spawn.y", 100);
        int spawnZ = plugin.getConfigManager().getConfig().getInt("hub.spawn.z", 0);
        spawnLocation = new Location(hubWorld, spawnX + 0.5, spawnY, spawnZ + 0.5);

        // Build the spawn platform the first time (detect via a marker block)
        if (!hasSpawnPlatform()) {
            buildSpawnPlatform(spawnX, spawnY, spawnZ);
        }

        // Set the world spawn point and keep players from wandering into the void
        hubWorld.setSpawnLocation(spawnX, spawnY, spawnZ);
    }

    /**
     * Checks whether the spawn platform has already been built by looking for
     * the marker block (a gold block at the center).
     */
    private boolean hasSpawnPlatform() {
        int x = plugin.getConfigManager().getConfig().getInt("hub.spawn.x", 0);
        int y = plugin.getConfigManager().getConfig().getInt("hub.spawn.y", 100);
        int z = plugin.getConfigManager().getConfig().getInt("hub.spawn.z", 0);
        return hubWorld.getBlockAt(x, y - 1, z).getType() == Material.GOLD_BLOCK;
    }

    /**
     * Builds a decorative spawn platform: a circular stone-brick floor with
     * glowstone accents, a few pillars, and signs pointing to the game modes.
     */
    private void buildSpawnPlatform(int cx, int cy, int cz) {
        // 11x11 floor of stone bricks with a glowstone ring
        for (int x = -5; x <= 5; x++) {
            for (int z = -5; z <= 5; z++) {
                double dist = Math.sqrt(x * x + z * z);
                if (dist <= 5.5) {
                    Material mat = (Math.abs(dist - 4.5) < 0.75) ? Material.GLOWSTONE : Material.STONE_BRICKS;
                    hubWorld.getBlockAt(cx + x, cy - 1, cz + z).setType(mat);
                }
            }
        }
        // Center marker
        hubWorld.getBlockAt(cx, cy - 1, cz).setType(Material.GOLD_BLOCK);

        // Four corner pillars (3 blocks tall) with lanterns on top
        int[][] corners = {{-4, -4}, {4, -4}, {-4, 4}, {4, 4}};
        for (int[] c : corners) {
            for (int y = 0; y < 3; y++) {
                hubWorld.getBlockAt(cx + c[0], cy + y, cz + c[1]).setType(Material.QUARTZ_PILLAR);
            }
            hubWorld.getBlockAt(cx + c[0], cy + 3, cz + c[1]).setType(Material.SEA_LANTERN);
        }

        plugin.getLogger().info("Built hub spawn platform at " + cx + "/" + cy + "/" + cz);
    }

    public World getHubWorld() {
        return hubWorld;
    }

    public Location getSpawnLocation() {
        return spawnLocation;
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
