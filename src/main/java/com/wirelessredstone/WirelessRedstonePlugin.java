package com.wirelessredstone;

import com.wirelessredstone.command.WirelessCommand;
import com.wirelessredstone.listener.BulbBreakListener;
import com.wirelessredstone.listener.BulbPlaceListener;
import com.wirelessredstone.listener.ChestBreakListener;
import com.wirelessredstone.listener.ChestInventoryListener;
import com.wirelessredstone.listener.ChestPlaceListener;
import com.wirelessredstone.listener.GUIListener;
import com.wirelessredstone.listener.WireViewListener;
import com.wirelessredstone.manager.DebugManager;
import com.wirelessredstone.manager.LinkedBulbManager;
import com.wirelessredstone.manager.LinkedChestManager;
import com.wirelessredstone.manager.WireViewManager;
import com.wirelessredstone.task.BulbSyncTask;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public class WirelessRedstonePlugin extends JavaPlugin {

    private static WirelessRedstonePlugin instance;
    private LinkedBulbManager bulbManager;
    private LinkedChestManager chestManager;
    private WireViewManager wireViewManager;
    private DebugManager debugManager;
    private BukkitTask syncTask;

    @Override
    public void onEnable() {
        instance = this;
        bulbManager = new LinkedBulbManager(this);
        chestManager = new LinkedChestManager(this);
        wireViewManager = new WireViewManager(this, bulbManager);
        debugManager = new DebugManager();

        registerCommands();
        registerListeners();
        startSyncTask();

        getLogger().info("WirelessRedstone has been enabled!");
    }

    @Override
    public void onDisable() {
        if (syncTask != null) {
            syncTask.cancel();
        }
        if (wireViewManager != null) {
            wireViewManager.cleanupAll();
        }
        if (bulbManager != null) {
            bulbManager.saveData();
        }
        if (chestManager != null) {
            chestManager.saveData();
        }
        getLogger().info("WirelessRedstone has been disabled!");
    }

    private void registerCommands() {
        var command = getCommand("wireless");
        if (command != null) {
            var wirelessCommand = new WirelessCommand(bulbManager, chestManager, wireViewManager, debugManager);
            command.setExecutor(wirelessCommand);
            command.setTabCompleter(wirelessCommand);
        }
    }

    private void registerListeners() {
        var pluginManager = getServer().getPluginManager();
        pluginManager.registerEvents(new BulbPlaceListener(bulbManager), this);
        pluginManager.registerEvents(new BulbBreakListener(bulbManager), this);
        pluginManager.registerEvents(new ChestPlaceListener(chestManager), this);
        pluginManager.registerEvents(new ChestBreakListener(chestManager), this);
        pluginManager.registerEvents(new ChestInventoryListener(chestManager), this);
        pluginManager.registerEvents(new GUIListener(bulbManager), this);
        pluginManager.registerEvents(new WireViewListener(wireViewManager), this);
    }

    private void startSyncTask() {
        syncTask = new BulbSyncTask(bulbManager, debugManager).runTaskTimer(this, 1L, 1L);
    }

    public static WirelessRedstonePlugin getInstance() {
        return instance;
    }

    public LinkedBulbManager getBulbManager() {
        return bulbManager;
    }

    public LinkedChestManager getChestManager() {
        return chestManager;
    }

    public WireViewManager getWireViewManager() {
        return wireViewManager;
    }

    public DebugManager getDebugManager() {
        return debugManager;
    }
}
