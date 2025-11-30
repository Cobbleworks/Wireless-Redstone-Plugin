package com.wirelessredstone.manager;

import com.wirelessredstone.WirelessRedstonePlugin;
import com.wirelessredstone.model.BulbPair;
import net.kyori.adventure.text.format.NamedTextColor;
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
    private final Set<UUID> playersWithWireView = ConcurrentHashMap.newKeySet();
    private final Map<UUID, Set<UUID>> playerGlowEntities = new ConcurrentHashMap<>();

    private static final String WIREVIEW_TEAM_PREFIX = "wv_";

    public WireViewManager(WirelessRedstonePlugin plugin, LinkedBulbManager bulbManager) {
        this.plugin = plugin;
        this.bulbManager = bulbManager;
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

        for (BulbPair pair : bulbManager.getAllPlacedPairs()) {
            NamedTextColor pairColor = colors[colorIndex % colors.length];
            colorIndex++;

            String teamName = WIREVIEW_TEAM_PREFIX + pair.getPairId().toString().substring(0, 8);
            Team team = scoreboard.getTeam(teamName);
            if (team == null) {
                team = scoreboard.registerNewTeam(teamName);
            }
            team.color(pairColor);

            if (pair.getLocation1() != null) {
                Entity entity = spawnGlowEntity(pair.getLocation1(), player, team);
                if (entity != null) {
                    entityIds.add(entity.getUniqueId());
                }
            }

            if (pair.getLocation2() != null) {
                Entity entity = spawnGlowEntity(pair.getLocation2(), player, team);
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
            if (team.getName().startsWith(WIREVIEW_TEAM_PREFIX)) {
                teamsToRemove.add(team);
            }
        }
        teamsToRemove.forEach(Team::unregister);
    }

    public void cleanupPlayer(Player player) {
        disableWireView(player);
    }

    public void cleanupAll() {
        for (var world : plugin.getServer().getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (entity.getScoreboardTags().contains("wireview_glow")) {
                    entity.remove();
                }
            }
        }
        playersWithWireView.clear();
        playerGlowEntities.clear();
    }

    public void refreshAllPlayers() {
        for (UUID playerId : playersWithWireView) {
            Player player = plugin.getServer().getPlayer(playerId);
            if (player != null && player.isOnline()) {
                refreshGlowingEntities(player);
            }
        }
    }
}
