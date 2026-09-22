package com.example.skyblock.island;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class IslandManager {

    private final org.bukkit.plugin.Plugin plugin;
    private final File dataFile;

    private final Map<UUID, Island> islandsByOwner = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> memberToOwner = new ConcurrentHashMap<>();
    // pending invite: pozvani igrac -> vlasnik otoka koji ga je pozvao
    private final Map<UUID, UUID> pendingInvites = new ConcurrentHashMap<>();

    private final String worldName;
    private final int spacing;
    private final int islandY;
    private final int maxMembers;
    private final int protectionRadius;

    private World skyblockWorld;
    private int nextGridIndex = 0;

    public IslandManager(org.bukkit.plugin.Plugin plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "islands.yml");

        FileConfiguration cfg = plugin.getConfig();
        this.worldName = cfg.getString("world-name", "skyblock_world");
        this.spacing = cfg.getInt("island-spacing", 250);
        this.islandY = cfg.getInt("island-y", 100);
        this.maxMembers = cfg.getInt("max-members", 2);
        this.protectionRadius = cfg.getInt("protection-radius", 60);
    }

    public void setupWorld() {
        World existing = Bukkit.getWorld(worldName);
        if (existing != null) {
            skyblockWorld = existing;
            return;
        }
        WorldCreator creator = new WorldCreator(worldName);
        creator.type(WorldType.FLAT);
        creator.generateStructures(false);
        skyblockWorld = creator.createWorld();
        if (skyblockWorld != null) {
            skyblockWorld.setSpawnFlags(false, false);
        }
    }

    public World getSkyblockWorld() {
        return skyblockWorld;
    }

    public int getMaxMembers() {
        return maxMembers;
    }

    // ==================== KREIRANJE ====================

    public boolean hasIsland(UUID playerId) {
        return islandsByOwner.containsKey(playerId) || memberToOwner.containsKey(playerId);
    }

    public java.util.Collection<Island> getAllIslands() {
        return islandsByOwner.values();
    }

    public Island getIslandOf(UUID playerId) {
        if (islandsByOwner.containsKey(playerId)) {
            return islandsByOwner.get(playerId);
        }
        UUID owner = memberToOwner.get(playerId);
        if (owner != null) {
            return islandsByOwner.get(owner);
        }
        return null;
    }

    public Island createIsland(Player owner, IslandType type) {
        if (hasIsland(owner.getUniqueId())) {
            return null;
        }
        Location center = nextGridLocation();
        Location spawnPoint = IslandGenerator.generate(type, center);

        Island island = new Island(UUID.randomUUID(), owner.getUniqueId(), type, center, protectionRadius);
        islandsByOwner.put(owner.getUniqueId(), island);

        owner.teleport(spawnPoint);
        saveAsync();
        return island;
    }

    public boolean deleteIsland(UUID ownerId) {
        Island island = islandsByOwner.remove(ownerId);
        if (island == null) return false;
        for (UUID member : island.getMembers()) {
            memberToOwner.remove(member);
        }
        saveAsync();
        return true;
    }

    /**
     * Brise trenutni otok vlasnika i odmah generira novi (na novoj lokaciji),
     * zadrzavajuci iste clanove tima. Koristi se za komandu /island reset.
     * Vraca novi Island objekt, ili null ako vlasnik nema otok.
     */
    public Island resetIsland(Player owner) {
        Island oldIsland = islandsByOwner.remove(owner.getUniqueId());
        if (oldIsland == null) return null;

        java.util.Set<UUID> members = new java.util.LinkedHashSet<>(oldIsland.getMembers());

        Location center = nextGridLocation();
        Location spawnPoint = IslandGenerator.generate(oldIsland.getType(), center);

        Island newIsland = new Island(UUID.randomUUID(), owner.getUniqueId(), oldIsland.getType(), center, protectionRadius);
        newIsland.getMembers().addAll(members);
        islandsByOwner.put(owner.getUniqueId(), newIsland);

        owner.teleport(spawnPoint);
        for (UUID memberId : members) {
            Player memberPlayer = Bukkit.getPlayer(memberId);
            if (memberPlayer != null) {
                memberPlayer.teleport(spawnPoint);
            }
        }

        saveAsync();
        return newIsland;
    }

    /** Sljedeca slobodna pozicija na spiralnoj mrezi, dovoljno udaljena od ostalih otoka. */
    private Location nextGridLocation() {
        int index = nextGridIndex++;
        int[] coords = spiralCoords(index);
        int x = coords[0] * spacing;
        int z = coords[1] * spacing;
        return new Location(skyblockWorld, x + 0.5, islandY, z + 0.5);
    }

    private int[] spiralCoords(int index) {
        // Jednostavna kvadratna spirala oko (0,0)
        if (index == 0) return new int[]{0, 0};
        int layer = (int) Math.ceil((Math.sqrt(index + 1) - 1) / 2);
        int maxInLayer = (2 * layer + 1) * (2 * layer + 1) - 1;
        int minInLayer = (2 * layer - 1) * (2 * layer - 1) - 1;
        int sideLength = 2 * layer;
        int positionInLayer = index - minInLayer - 1;
        int side = positionInLayer / sideLength;
        int offset = positionInLayer % sideLength;

        int x, z;
        switch (side) {
            case 0: x = layer; z = -layer + offset; break;
            case 1: x = layer - offset; z = layer; break;
            case 2: x = -layer; z = layer - offset; break;
            default: x = -layer + offset; z = -layer; break;
        }
        return new int[]{x, z};
    }

    // ==================== CLANOVI / POZIVNICE ====================

    public boolean invite(Player owner, Player target) {
        Island island = islandsByOwner.get(owner.getUniqueId());
        if (island == null) return false;
        if (island.getResidentCount() >= maxMembers) return false;
        if (hasIsland(target.getUniqueId())) return false;
        pendingInvites.put(target.getUniqueId(), owner.getUniqueId());
        return true;
    }

    public UUID getPendingInviteOwner(UUID targetId) {
        return pendingInvites.get(targetId);
    }

    public boolean acceptInvite(Player target) {
        UUID ownerId = pendingInvites.remove(target.getUniqueId());
        if (ownerId == null) return false;
        Island island = islandsByOwner.get(ownerId);
        if (island == null) return false;
        if (island.getResidentCount() >= maxMembers) return false;

        island.getMembers().add(target.getUniqueId());
        memberToOwner.put(target.getUniqueId(), ownerId);
        target.teleport(island.getCenter());
        saveAsync();
        return true;
    }

    public void denyInvite(UUID targetId) {
        pendingInvites.remove(targetId);
    }

    public boolean kickMember(Player owner, UUID targetId) {
        Island island = islandsByOwner.get(owner.getUniqueId());
        if (island == null) return false;
        boolean removed = island.getMembers().remove(targetId);
        if (removed) {
            memberToOwner.remove(targetId);
            saveAsync();
        }
        return removed;
    }

    public boolean leaveIsland(Player member) {
        UUID ownerId = memberToOwner.remove(member.getUniqueId());
        if (ownerId == null) return false;
        Island island = islandsByOwner.get(ownerId);
        if (island != null) {
            island.getMembers().remove(member.getUniqueId());
        }
        saveAsync();
        return true;
    }

    // ==================== PERSISTENCIJA ====================

    public void loadAll() {
        if (!dataFile.exists()) return;
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(dataFile);
        List<Map<?, ?>> raw = yml.getMapList("islands");
        int maxIndex = -1;
        for (Map<?, ?> entry : raw) {
            try {
                UUID id = UUID.fromString((String) entry.get("id"));
                UUID owner = UUID.fromString((String) entry.get("owner"));
                // Backward-kompatibilnost: stare verzije su imale NORMAL/DESERT kao odvojene tipove,
                // sad postoji samo CLASSIC.
                IslandType type = IslandType.CLASSIC;
                double x = ((Number) entry.get("x")).doubleValue();
                double y = ((Number) entry.get("y")).doubleValue();
                double z = ((Number) entry.get("z")).doubleValue();
                int gridIndex = entry.containsKey("gridIndex") ? ((Number) entry.get("gridIndex")).intValue() : -1;

                Location center = new Location(skyblockWorld, x, y, z);
                Island island = new Island(id, owner, type, center, protectionRadius);

                @SuppressWarnings("unchecked")
                List<String> membersList = (List<String>) entry.get("members");
                if (membersList != null) {
                    for (String m : membersList) {
                        UUID memberId = UUID.fromString(m);
                        island.getMembers().add(memberId);
                        memberToOwner.put(memberId, owner);
                    }
                }

                islandsByOwner.put(owner, island);
                if (gridIndex > maxIndex) maxIndex = gridIndex;
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Neuspjelo ucitavanje jednog otoka iz islands.yml", e);
            }
        }
        nextGridIndex = maxIndex + 1;
    }

    public void saveAsync() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, this::saveNow);
    }

    public void saveNow() {
        YamlConfiguration yml = new YamlConfiguration();
        List<Map<String, Object>> list = new java.util.ArrayList<>();
        int index = 0;
        for (Island island : islandsByOwner.values()) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", island.getId().toString());
            map.put("owner", island.getOwner().toString());
            map.put("type", island.getType().name());
            map.put("x", island.getCenter().getX());
            map.put("y", island.getCenter().getY());
            map.put("z", island.getCenter().getZ());
            map.put("gridIndex", index++);
            List<String> members = new java.util.ArrayList<>();
            for (UUID m : island.getMembers()) members.add(m.toString());
            map.put("members", members);
            list.add(map);
        }
        yml.set("islands", list);
        try {
            if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
            yml.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Neuspjelo spremanje islands.yml", e);
        }
    }
}
