package com.wirelessredstone.manager;

import com.wirelessredstone.WirelessRedstonePlugin;
import com.wirelessredstone.model.BaseGroup;
import com.wirelessredstone.model.BulbGroup;
import com.wirelessredstone.model.ChestGroup;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Shulker;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class WireViewManager {

    private final WirelessRedstonePlugin plugin;
    private final LinkedBulbManager bulbManager;
    private final LinkedChestManager chestManager;
    private final Set<UUID> playersWithWireView = ConcurrentHashMap.newKeySet();
    private final Map<UUID, Set<UUID>> playerGlowEntities = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> playerSingleGroupView = new ConcurrentHashMap<>();

    private static final String WIREVIEW_TEAM_PREFIX = "wv_";
    private static final String WIREVIEW_CHEST_TEAM_PREFIX = "wvc_";
    private static final String WIREVIEW_SINGLE_TEAM = "wv_single";
    private static final NamedTextColor[] BULB_GROUP_COLORS = {
        NamedTextColor.AQUA,
        NamedTextColor.GOLD,
        NamedTextColor.GREEN,
        NamedTextColor.RED,
        NamedTextColor.LIGHT_PURPLE,
        NamedTextColor.YELLOW,
        NamedTextColor.WHITE,
        NamedTextColor.BLUE
    };

    public WireViewManager(WirelessRedstonePlugin plugin, LinkedBulbManager bulbManager, LinkedChestManager chestManager) {
        this.plugin = plugin;
        this.bulbManager = bulbManager;
        this.chestManager = chestManager;
    }

    public boolean toggleWireView(Player player) {
        UUID playerId = player.getUniqueId();
        
        if (playersWithWireView.contains(playerId)) {
            disableWireView(player);
            return false;
        } else {
            enableWireView(player);
            return true;
        }
    }

    public boolean hasWireViewEnabled(Player player) {
        return playersWithWireView.contains(player.getUniqueId());
    }

    public void enableWireView(Player player) {
        UUID playerId = player.getUniqueId();
        playersWithWireView.add(playerId);
        refreshGlowingEntities(player);
    }

    public void disableWireView(Player player) {
        UUID playerId = player.getUniqueId();
        playersWithWireView.remove(playerId);
        removeAllGlowEntities(player);
    }

    public void refreshGlowingEntities(Player player) {
        if (!playersWithWireView.contains(player.getUniqueId())) {
            return;
        }

        removeAllGlowEntities(player);

        Set<UUID> entityIds = ConcurrentHashMap.newKeySet();
        playerGlowEntities.put(player.getUniqueId(), entityIds);

        Scoreboard scoreboard = player.getScoreboard();
        int colorIndex = 0;
        // Spawn glow entities for bulb groups
        for (BulbGroup group : bulbManager.getAllPlacedGroups()) {
            NamedTextColor groupColor = BULB_GROUP_COLORS[colorIndex % BULB_GROUP_COLORS.length];
            colorIndex++;

            String teamName = WIREVIEW_TEAM_PREFIX + group.getGroupId().toString().substring(0, 8);
            Team team = scoreboard.getTeam(teamName);
            if (team == null) {
                team = scoreboard.registerNewTeam(teamName);
            }
            team.color(groupColor);

            for (Location loc : group.getPlacedLocations()) {
                Entity entity = spawnGlowEntity(loc, player, team);
                if (entity != null) {
                    entityIds.add(entity.getUniqueId());
                }
            }
        }

        // Spawn glow entities for chest groups with different color palette
        int chestColorIndex = 0;
        NamedTextColor[] chestColors = {
            NamedTextColor.DARK_AQUA,
            NamedTextColor.DARK_GREEN,
            NamedTextColor.DARK_RED,
            NamedTextColor.DARK_PURPLE,
            NamedTextColor.DARK_BLUE,
            NamedTextColor.DARK_GRAY,
            NamedTextColor.GRAY
        };

        for (ChestGroup group : chestManager.getAllPlacedGroups()) {
            NamedTextColor groupColor = chestColors[chestColorIndex % chestColors.length];
            chestColorIndex++;

            String teamName = WIREVIEW_CHEST_TEAM_PREFIX + group.getGroupId().toString().substring(0, 8);
            Team team = scoreboard.getTeam(teamName);
            if (team == null) {
                team = scoreboard.registerNewTeam(teamName);
            }
            team.color(groupColor);

            for (Location loc : group.getPlacedLocations()) {
                Entity entity = spawnGlowEntity(loc, player, team);
                if (entity != null) {
                    entityIds.add(entity.getUniqueId());
                }
            }
        }
    }

    private Entity spawnGlowEntity(Location blockLocation, Player player, Team team) {
        if (blockLocation.getWorld() == null) return null;

        Location spawnLoc = blockLocation.clone().add(0.5, 0, 0.5);

        Shulker shulker = (Shulker) blockLocation.getWorld().spawnEntity(spawnLoc, EntityType.SHULKER);
        shulker.setAI(false);
        shulker.setInvulnerable(true);
        shulker.setSilent(true);
        shulker.setGravity(false);
        shulker.setInvisible(true);
        shulker.setGlowing(true);
        shulker.setPersistent(false);
        shulker.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 0, false, false));

        shulker.addScoreboardTag("wireview_glow");
        shulker.addScoreboardTag("wireview_player_" + player.getUniqueId());

        if (team != null) {
            team.addEntity(shulker);
        }

        return shulker;
    }

    public static Color getBulbGroupParticleColor(UUID groupId, List<BulbGroup> groups) {
        int colorIndex = 0;
        for (BulbGroup group : groups) {
            if (group.getGroupId().equals(groupId)) {
                NamedTextColor color = BULB_GROUP_COLORS[colorIndex % BULB_GROUP_COLORS.length];
                return Color.fromRGB(color.value());
            }
            colorIndex++;
        }
        return Color.fromRGB(NamedTextColor.AQUA.value());
    }

    private void removeAllGlowEntities(Player player) {
        UUID playerId = player.getUniqueId();
        Set<UUID> entityIds = playerGlowEntities.remove(playerId);

        for (var world : plugin.getServer().getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (entity.getScoreboardTags().contains("wireview_player_" + playerId)) {
                    entity.remove();
                }
            }
        }

        Scoreboard scoreboard = player.getScoreboard();
        Set<Team> teamsToRemove = new HashSet<>();
        for (Team team : scoreboard.getTeams()) {
            if (team.getName().startsWith(WIREVIEW_TEAM_PREFIX) || team.getName().startsWith(WIREVIEW_CHEST_TEAM_PREFIX)) {
                teamsToRemove.add(team);
            }
        }
        teamsToRemove.forEach(Team::unregister);
    }

    public void cleanupPlayer(Player player) {
        disableWireView(player);
        disableSingleGroupView(player);
    }

    public void cleanupAll() {
        for (var world : plugin.getServer().getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (entity.getScoreboardTags().contains("wireview_glow") 
                        || entity.getScoreboardTags().contains("wireview_single_glow")) {
                    entity.remove();
                }
            }
        }
        playersWithWireView.clear();
        playerGlowEntities.clear();
        playerSingleGroupView.clear();
    }

    public void refreshAllPlayers() {
        for (UUID playerId : playersWithWireView) {
            Player player = plugin.getServer().getPlayer(playerId);
            if (player != null && player.isOnline()) {
                refreshGlowingEntities(player);
            }
        }
    }

    /**
     * Enables wire view for a single specific group.
     * Legacy single-group overlay support.
     */
    public void enableSingleGroupView(Player player, UUID groupId, boolean isBulbGroup) {
        UUID playerId = player.getUniqueId();
        UUID existingGroupId = playerSingleGroupView.get(playerId);
        
        if (groupId.equals(existingGroupId)) {
            return;
        }
        
        disableSingleGroupView(player);
        playerSingleGroupView.put(playerId, groupId);
        refreshSingleGroupGlow(player, groupId, isBulbGroup);
    }

    /**
     * Disables the single group wire view for a player.
     */
    public void disableSingleGroupView(Player player) {
        UUID playerId = player.getUniqueId();
        if (!playerSingleGroupView.containsKey(playerId)) return;
        
        playerSingleGroupView.remove(playerId);
        removeSingleGroupGlowEntities(player);
    }

    /**
     * Checks if a player has single group view enabled.
     */
    public boolean hasSingleGroupViewEnabled(Player player) {
        return playerSingleGroupView.containsKey(player.getUniqueId());
    }

    /**
     * Gets the group ID of the single group view for a player.
     */
    public UUID getSingleGroupViewId(Player player) {
        return playerSingleGroupView.get(player.getUniqueId());
    }

    private void refreshSingleGroupGlow(Player player, UUID groupId, boolean isBulbGroup) {
        Set<UUID> entityIds = ConcurrentHashMap.newKeySet();
        String tagSuffix = "_single_" + player.getUniqueId();
        
        Scoreboard scoreboard = player.getScoreboard();
        String teamName = WIREVIEW_SINGLE_TEAM + "_" + player.getUniqueId().toString().substring(0, 8);
        Team team = scoreboard.getTeam(teamName);
        if (team == null) {
            team = scoreboard.registerNewTeam(teamName);
        }
        team.color(isBulbGroup ? NamedTextColor.AQUA : NamedTextColor.GOLD);

        BaseGroup group = isBulbGroup 
            ? bulbManager.getGroupById(groupId).orElse(null)
            : chestManager.getGroupById(groupId).orElse(null);
        
        if (group == null) return;

        for (Location loc : group.getPlacedLocations()) {
            Entity entity = spawnSingleGroupGlowEntity(loc, player, team, tagSuffix);
            if (entity != null) {
                entityIds.add(entity.getUniqueId());
            }
        }
    }

    private Entity spawnSingleGroupGlowEntity(Location blockLocation, Player player, Team team, String tagSuffix) {
        if (blockLocation.getWorld() == null) return null;

        Location spawnLoc = blockLocation.clone().add(0.5, 0, 0.5);

        Shulker shulker = (Shulker) blockLocation.getWorld().spawnEntity(spawnLoc, EntityType.SHULKER);
        shulker.setAI(false);
        shulker.setInvulnerable(true);
        shulker.setSilent(true);
        shulker.setGravity(false);
        shulker.setInvisible(true);
        shulker.setGlowing(true);
        shulker.setPersistent(false);
        shulker.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 0, false, false));

        shulker.addScoreboardTag("wireview_single_glow");
        shulker.addScoreboardTag("wireview_single" + tagSuffix);

        if (team != null) {
            team.addEntity(shulker);
        }

        return shulker;
    }

    private void removeSingleGroupGlowEntities(Player player) {
        UUID playerId = player.getUniqueId();
        String tagSuffix = "_single_" + playerId;

        for (var world : plugin.getServer().getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (entity.getScoreboardTags().contains("wireview_single" + tagSuffix)) {
                    entity.remove();
                }
            }
        }

        Scoreboard scoreboard = player.getScoreboard();
        String teamName = WIREVIEW_SINGLE_TEAM + "_" + playerId.toString().substring(0, 8);
        Team team = scoreboard.getTeam(teamName);
        if (team != null) {
            team.unregister();
        }
    }

    /**
     * Refreshes the single group view for a player (e.g., after a block is added/removed).
     */
    public void refreshSingleGroupView(Player player) {
        UUID playerId = player.getUniqueId();
        UUID groupId = playerSingleGroupView.get(playerId);
        if (groupId == null) return;
        
        boolean isBulbGroup = bulbManager.getGroupById(groupId).isPresent();
        removeSingleGroupGlowEntities(player);
        refreshSingleGroupGlow(player, groupId, isBulbGroup);
    }

    /**
     * Refreshes single group view for all players viewing a specific group.
     */
    public void refreshSingleGroupViewForGroup(UUID groupId) {
        for (Map.Entry<UUID, UUID> entry : playerSingleGroupView.entrySet()) {
            if (groupId.equals(entry.getValue())) {
                Player player = plugin.getServer().getPlayer(entry.getKey());
                if (player != null && player.isOnline()) {
                    refreshSingleGroupView(player);
                }
            }
        }
    }
}
