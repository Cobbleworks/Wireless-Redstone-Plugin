package com.wirelessredstone.task;

import com.wirelessredstone.item.CircuitAnalyserFactory;
import com.wirelessredstone.manager.LinkedBulbManager;
import com.wirelessredstone.manager.LinkedChestManager;
import com.wirelessredstone.model.BulbGroup;
import com.wirelessredstone.model.ChestGroup;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Shulker;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Task that manages the "mini wireview" effect when players hold a Circuit Analyser.
 * Shows glowing entities for wireless blocks within 8 blocks of the player.
 */
public class CircuitAnalyserTask extends BukkitRunnable {

    private static final int SCAN_RADIUS = 8;
    private static final String ANALYSER_TEAM_PREFIX = "ca_";
    private static final String ANALYSER_CHEST_TEAM_PREFIX = "cac_";
    private static final String ANALYSER_GLOW_TAG = "analyser_glow";

    private final JavaPlugin plugin;
    private final LinkedBulbManager bulbManager;
    private final LinkedChestManager chestManager;
    private final Map<UUID, Set<UUID>> playerGlowEntities = new ConcurrentHashMap<>();
    private final Set<UUID> playersWithAnalyser = ConcurrentHashMap.newKeySet();

    public CircuitAnalyserTask(JavaPlugin plugin, LinkedBulbManager bulbManager, LinkedChestManager chestManager) {
        this.plugin = plugin;
        this.bulbManager = bulbManager;
        this.chestManager = chestManager;
    }

    @Override
    public void run() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            ItemStack mainHand = player.getInventory().getItemInMainHand();
            ItemStack offHand = player.getInventory().getItemInOffHand();
            
            boolean holdingAnalyser = CircuitAnalyserFactory.isCircuitAnalyser(mainHand) 
                    || CircuitAnalyserFactory.isCircuitAnalyser(offHand);
            
            UUID playerId = player.getUniqueId();
            
            if (holdingAnalyser) {
                if (!playersWithAnalyser.contains(playerId)) {
                    // Player just started holding analyser
                    playersWithAnalyser.add(playerId);
                }
                // Update glow entities for nearby wireless blocks
                updateNearbyGlowEntities(player);
            } else {
                if (playersWithAnalyser.contains(playerId)) {
                    // Player stopped holding analyser
                    playersWithAnalyser.remove(playerId);
                    removeAllGlowEntities(player);
                }
            }
        }
    }

    private void updateNearbyGlowEntities(Player player) {
        Location playerLoc = player.getLocation();
        Set<UUID> currentEntityIds = playerGlowEntities.computeIfAbsent(player.getUniqueId(), k -> ConcurrentHashMap.newKeySet());
        Set<Location> desiredLocations = new HashSet<>();
        Map<Location, Object> locationToGroup = new HashMap<>();

        // Find all wireless blocks within radius
        for (BulbGroup group : bulbManager.getAllPlacedGroups()) {
            for (Location loc : group.getPlacedLocations()) {
                if (loc.getWorld() != null && loc.getWorld().equals(playerLoc.getWorld())) {
                    if (loc.distance(playerLoc) <= SCAN_RADIUS) {
                        desiredLocations.add(loc);
                        locationToGroup.put(loc, group);
                    }
                }
            }
        }

        for (ChestGroup group : chestManager.getAllPlacedGroups()) {
            for (Location loc : group.getPlacedLocations()) {
                if (loc.getWorld() != null && loc.getWorld().equals(playerLoc.getWorld())) {
                    if (loc.distance(playerLoc) <= SCAN_RADIUS) {
                        desiredLocations.add(loc);
                        locationToGroup.put(loc, group);
                    }
                }
            }
        }

        // Remove entities that are no longer in range
        Set<UUID> toRemove = new HashSet<>();
        for (UUID entityId : currentEntityIds) {
            Entity entity = plugin.getServer().getEntity(entityId);
            if (entity == null || !entity.isValid()) {
                toRemove.add(entityId);
                continue;
            }
            
            Location entityLoc = entity.getLocation().getBlock().getLocation();
            if (!desiredLocations.contains(entityLoc)) {
                entity.remove();
                toRemove.add(entityId);
            } else {
                // Already have entity at this location, don't need to spawn new one
                desiredLocations.remove(entityLoc);
            }
        }
        currentEntityIds.removeAll(toRemove);

        // Spawn new entities for remaining desired locations
        Scoreboard scoreboard = player.getScoreboard();
        
        for (Location loc : desiredLocations) {
            Object group = locationToGroup.get(loc);
            Team team = getOrCreateTeam(scoreboard, group);
            
            Entity entity = spawnGlowEntity(loc, player, team);
            if (entity != null) {
                currentEntityIds.add(entity.getUniqueId());
            }
        }
    }

    private Team getOrCreateTeam(Scoreboard scoreboard, Object group) {
        String teamName;
        NamedTextColor color;

        if (group instanceof BulbGroup bulbGroup) {
            teamName = ANALYSER_TEAM_PREFIX + bulbGroup.getGroupId().toString().substring(0, 8);
            color = getColorForBulbGroup(bulbGroup);
        } else if (group instanceof ChestGroup chestGroup) {
            teamName = ANALYSER_CHEST_TEAM_PREFIX + chestGroup.getGroupId().toString().substring(0, 8);
            color = getColorForChestGroup(chestGroup);
        } else {
            return null;
        }

        Team team = scoreboard.getTeam(teamName);
        if (team == null) {
            team = scoreboard.registerNewTeam(teamName);
        }
        team.color(color);
        return team;
    }

    private NamedTextColor getColorForBulbGroup(BulbGroup group) {
        // Use a hash of the group ID to pick a consistent color
        int hash = group.getGroupId().hashCode();
        NamedTextColor[] colors = {
            NamedTextColor.AQUA,
            NamedTextColor.GOLD,
            NamedTextColor.GREEN,
            NamedTextColor.RED,
            NamedTextColor.LIGHT_PURPLE,
            NamedTextColor.YELLOW,
            NamedTextColor.WHITE,
            NamedTextColor.BLUE
        };
        return colors[Math.abs(hash) % colors.length];
    }

    private NamedTextColor getColorForChestGroup(ChestGroup group) {
        int hash = group.getGroupId().hashCode();
        NamedTextColor[] colors = {
            NamedTextColor.DARK_AQUA,
            NamedTextColor.DARK_GREEN,
            NamedTextColor.DARK_RED,
            NamedTextColor.DARK_PURPLE,
            NamedTextColor.DARK_BLUE,
            NamedTextColor.DARK_GRAY,
            NamedTextColor.GRAY
        };
        return colors[Math.abs(hash) % colors.length];
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

        shulker.addScoreboardTag(ANALYSER_GLOW_TAG);
        shulker.addScoreboardTag("analyser_player_" + player.getUniqueId());

        if (team != null) {
            team.addEntity(shulker);
        }

        return shulker;
    }

    private void removeAllGlowEntities(Player player) {
        UUID playerId = player.getUniqueId();
        Set<UUID> entityIds = playerGlowEntities.remove(playerId);

        // Remove all entities tagged for this player
        for (var world : plugin.getServer().getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (entity.getScoreboardTags().contains("analyser_player_" + playerId)) {
                    entity.remove();
                }
            }
        }

        // Clean up teams
        Scoreboard scoreboard = player.getScoreboard();
        Set<Team> teamsToRemove = new HashSet<>();
        for (Team team : scoreboard.getTeams()) {
            if (team.getName().startsWith(ANALYSER_TEAM_PREFIX) || team.getName().startsWith(ANALYSER_CHEST_TEAM_PREFIX)) {
                if (team.getEntries().isEmpty()) {
                    teamsToRemove.add(team);
                }
            }
        }
        teamsToRemove.forEach(Team::unregister);
    }

    /**
     * Called when a player leaves the server or on plugin disable.
     */
    public void cleanupPlayer(Player player) {
        playersWithAnalyser.remove(player.getUniqueId());
        removeAllGlowEntities(player);
    }

    /**
     * Cleans up all glow entities when the plugin is disabled.
     */
    public void cleanupAll() {
        for (var world : plugin.getServer().getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (entity.getScoreboardTags().contains(ANALYSER_GLOW_TAG)) {
                    entity.remove();
                }
            }
        }
        playersWithAnalyser.clear();
        playerGlowEntities.clear();
    }
}
