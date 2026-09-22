package com.example.skyblock.island;

import org.bukkit.Location;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

public class Island {

    private final UUID id;
    private UUID owner;
    private final Set<UUID> members = new LinkedHashSet<>(); // ne racunajuci vlasnika
    private final IslandType type;
    private final Location center; // sredina otoka (spawn tocka na otoku)
    private final int protectionRadius;

    public Island(UUID id, UUID owner, IslandType type, Location center, int protectionRadius) {
        this.id = id;
        this.owner = owner;
        this.type = type;
        this.center = center;
        this.protectionRadius = protectionRadius;
    }

    public UUID getId() {
        return id;
    }

    public UUID getOwner() {
        return owner;
    }

    public void setOwner(UUID owner) {
        this.owner = owner;
    }

    public Set<UUID> getMembers() {
        return members;
    }

    public IslandType getType() {
        return type;
    }

    public Location getCenter() {
        return center;
    }

    public int getProtectionRadius() {
        return protectionRadius;
    }

    /** Vlasnik + svi clanovi. */
    public boolean isPartOfIsland(UUID playerId) {
        return owner.equals(playerId) || members.contains(playerId);
    }

    /** Trenutni broj ljudi na otoku (vlasnik + clanovi). */
    public int getResidentCount() {
        return 1 + members.size();
    }

    public boolean isInside(Location loc) {
        if (loc.getWorld() == null || center.getWorld() == null) return false;
        if (!loc.getWorld().equals(center.getWorld())) return false;
        double dx = loc.getX() - center.getX();
        double dz = loc.getZ() - center.getZ();
        return (dx * dx + dz * dz) <= ((double) protectionRadius * protectionRadius);
    }
}
