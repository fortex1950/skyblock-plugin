package com.example.skyblock.listeners;

import com.example.skyblock.island.Island;
import com.example.skyblock.island.IslandManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

import java.util.List;
import java.util.UUID;

/**
 * Sprjecava igrace koji nisu vlasnik/clan otoka da rade/lome blokove
 * unutar zasticenog radijusa tudjeg otoka. Ako lokacija ne pripada
 * niti jednom poznatom otoku (npr. van svih zasticenih zona), dopusteno je.
 */
public class ProtectionListener implements Listener {

    private final IslandManager manager;

    public ProtectionListener(IslandManager manager) {
        this.manager = manager;
    }

    @EventHandler(ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        if (!canModify(event.getPlayer(), event.getBlock().getLocation())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§cNe mozes rusiti blokove na tudjem otoku.");
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        if (!canModify(event.getPlayer(), event.getBlock().getLocation())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§cNe mozes graditi na tudjem otoku.");
        }
    }

    private boolean canModify(Player player, org.bukkit.Location loc) {
        if (player.hasPermission("skyblock.admin")) return true;
        if (loc.getWorld() == null || manager.getSkyblockWorld() == null) return true;
        if (!loc.getWorld().equals(manager.getSkyblockWorld())) return true; // izvan skyblock svijeta, ne diramo

        List<Island> islands = allIslands();
        for (Island island : islands) {
            if (island.isInside(loc)) {
                return island.isPartOfIsland(player.getUniqueId());
            }
        }
        return true; // nije unutar nijednog zasticenog podrucja
    }

    private List<Island> allIslands() {
        return new java.util.ArrayList<>(manager.getAllIslands());
    }
}
