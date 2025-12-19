package com.wirelessredstone.task;

import com.wirelessredstone.item.ConnectorToolFactory;
import com.wirelessredstone.manager.WireViewManager;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Task that automatically enables/disables single-group WireView based on whether the player
 * is holding a Connector Tool in their hand.
 */
public class ConnectorWireViewTask extends BukkitRunnable {

    private final JavaPlugin plugin;
    private final WireViewManager wireViewManager;
    private final Set<UUID> playersWithConnectorView = ConcurrentHashMap.newKeySet();

    public ConnectorWireViewTask(JavaPlugin plugin, WireViewManager wireViewManager) {
        this.plugin = plugin;
        this.wireViewManager = wireViewManager;
    }

    @Override
    public void run() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            ItemStack mainHand = player.getInventory().getItemInMainHand();
            ItemStack offHand = player.getInventory().getItemInOffHand();
            
            ItemStack connectorTool = null;
            if (ConnectorToolFactory.isConnectorTool(mainHand) && !ConnectorToolFactory.isCreationMode(mainHand)) {
                connectorTool = mainHand;
            } else if (ConnectorToolFactory.isConnectorTool(offHand) && !ConnectorToolFactory.isCreationMode(offHand)) {
                connectorTool = offHand;
            }
            
            UUID playerId = player.getUniqueId();
            boolean hadConnectorView = playersWithConnectorView.contains(playerId);
            
            if (connectorTool != null) {
                UUID groupId = ConnectorToolFactory.getGroupId(connectorTool);
                ConnectorToolFactory.GroupType groupType = ConnectorToolFactory.getGroupType(connectorTool);
                
                if (groupId != null && groupType != null) {
                    if (!hadConnectorView) {
                        playersWithConnectorView.add(playerId);
                    }
                    
                    UUID currentViewId = wireViewManager.getSingleGroupViewId(player);
                    if (!groupId.equals(currentViewId)) {
                        boolean isBulbGroup = groupType == ConnectorToolFactory.GroupType.BULB;
                        wireViewManager.enableSingleGroupView(player, groupId, isBulbGroup);
                    }
                }
            } else if (hadConnectorView) {
                playersWithConnectorView.remove(playerId);
                wireViewManager.disableSingleGroupView(player);
            }
        }
    }

    /**
     * Called when a player leaves the server.
     */
    public void cleanupPlayer(Player player) {
        UUID playerId = player.getUniqueId();
        if (playersWithConnectorView.remove(playerId)) {
            wireViewManager.disableSingleGroupView(player);
        }
    }

    /**
     * Cleans up all state when the plugin is disabled.
     */
    public void cleanupAll() {
        playersWithConnectorView.clear();
    }
}
