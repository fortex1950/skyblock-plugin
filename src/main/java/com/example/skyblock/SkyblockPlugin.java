package com.example.skyblock;

import com.example.skyblock.commands.IslandCommand;
import com.example.skyblock.island.IslandManager;
import com.example.skyblock.listeners.ProtectionListener;
import org.bukkit.plugin.java.JavaPlugin;

public class SkyblockPlugin extends JavaPlugin {

    private IslandManager islandManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.islandManager = new IslandManager(this);
        islandManager.setupWorld();
        islandManager.loadAll();

        IslandCommand islandCommand = new IslandCommand(this, islandManager);
        getCommand("island").setExecutor(islandCommand);
        getCommand("island").setTabCompleter(islandCommand);
        getCommand("reset").setExecutor(islandCommand);
        getCommand("reset").setTabCompleter(islandCommand);

        getServer().getPluginManager().registerEvents(new ProtectionListener(islandManager), this);

        getLogger().info("SkyblockPlugin ukljucen! Svijet: " + islandManager.getSkyblockWorld().getName());
    }

    @Override
    public void onDisable() {
        if (islandManager != null) {
            islandManager.saveNow();
        }
        getLogger().info("SkyblockPlugin iskljucen, podaci spremljeni.");
    }

    public IslandManager getIslandManager() {
        return islandManager;
    }
}
