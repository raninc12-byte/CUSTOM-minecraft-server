package com.gulis.skyblock.island;

import org.bukkit.Location;
import org.bukkit.World;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * In-memory model of a single Skyblock island.
 *
 * <p>Each island has an owner, a center location, a level, and a list of
 * member UUIDs. Instances are created by {@link IslandManager} and persisted
 * to the {@code islands} SQLite table.</p>
 */
public class Island {

    private final int id;
    private final UUID ownerUuid;
    private final List<UUID> members = new ArrayList<>();
    private Location center;
    private double level;
    private World world;

    public Island(int id, UUID ownerUuid, Location center, double level) {
        this.id = id;
        this.ownerUuid = ownerUuid;
        this.center = center;
        this.world = center.getWorld();
        this.level = level;
    }

    public int getId() { return id; }
    public UUID getOwnerUuid() { return ownerUuid; }
    public Location getCenter() { return center; }
    public void setCenter(Location center) { this.center = center; this.world = center.getWorld(); }
    public double getLevel() { return level; }
    public void setLevel(double level) { this.level = level; }
    public World getWorld() { return world; }

    public List<UUID> getMembers() { return members; }

    public void addMember(UUID uuid) {
        if (!members.contains(uuid)) {
            members.add(uuid);
        }
    }

    public void removeMember(UUID uuid) {
        members.remove(uuid);
    }

    public boolean isMember(UUID uuid) {
        return ownerUuid.equals(uuid) || members.contains(uuid);
    }

    /**
     * Returns the spawn/home location of the island (center, slightly above).
     */
    public Location getHome() {
        return center.clone().add(0.5, 1, 0.5);
    }
}
