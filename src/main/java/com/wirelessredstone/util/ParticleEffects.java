package com.wirelessredstone.util;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class ParticleEffects {

    private static final Particle.DustOptions SYNC_ON_DUST = new Particle.DustOptions(Color.fromRGB(0, 255, 200), 1.0f);
    private static final Particle.DustOptions SYNC_OFF_DUST = new Particle.DustOptions(Color.fromRGB(255, 100, 50), 1.0f);
    private static final Particle.DustOptions BREAK_DUST = new Particle.DustOptions(Color.fromRGB(255, 0, 0), 1.5f);
    private static final Particle.DustOptions AMBIENT_DUST = new Particle.DustOptions(Color.fromRGB(100, 200, 255), 0.6f);
    private static final Particle.DustOptions AMBIENT_LIT_DUST = new Particle.DustOptions(Color.fromRGB(255, 220, 100), 0.8f);
    private static final Particle.DustOptions CONNECT_DUST = new Particle.DustOptions(Color.fromRGB(50, 255, 50), 1.2f);
    private static final Particle.DustOptions DISCONNECT_DUST = new Particle.DustOptions(Color.fromRGB(255, 100, 100), 1.2f);

    public static void spawnSyncParticles(Location location, boolean lit) {
        World world = location.getWorld();
        if (world == null) return;

        Location center = location.clone().add(0.5, 0.5, 0.5);
        Particle.DustOptions dust = lit ? SYNC_ON_DUST : SYNC_OFF_DUST;

        world.spawnParticle(Particle.DUST, center, 15, 0.3, 0.3, 0.3, 0, dust);

        if (lit) {
            world.spawnParticle(Particle.END_ROD, center, 8, 0.2, 0.2, 0.2, 0.05);
        } else {
            world.spawnParticle(Particle.SMOKE, center, 10, 0.2, 0.2, 0.2, 0.02);
        }
    }

    public static void spawnBreakParticles(Location location) {
        World world = location.getWorld();
        if (world == null) return;

        Location center = location.clone().add(0.5, 0.5, 0.5);

        world.spawnParticle(Particle.DUST, center, 20, 0.4, 0.4, 0.4, 0, BREAK_DUST);
        world.spawnParticle(Particle.ELECTRIC_SPARK, center, 15, 0.3, 0.3, 0.3, 0.1);
    }

    public static void spawnAmbientParticles(Location location, boolean lit) {
        World world = location.getWorld();
        if (world == null) return;

        Location center = location.clone().add(0.5, 0.5, 0.5);
        Particle.DustOptions dust = lit ? AMBIENT_LIT_DUST : AMBIENT_DUST;

        // Subtle floating particles around the block
        world.spawnParticle(Particle.DUST, center, 2, 0.4, 0.4, 0.4, 0, dust);
        
        if (lit) {
            // Additional glow effect when lit
            world.spawnParticle(Particle.END_ROD, center, 1, 0.3, 0.3, 0.3, 0.01);
        }
    }

    public static void spawnTriggerParticles(Location location, boolean lit) {
        World world = location.getWorld();
        if (world == null) return;

        Location center = location.clone().add(0.5, 0.5, 0.5);

        if (lit) {
            // Bright burst when turning on
            world.spawnParticle(Particle.END_ROD, center, 20, 0.3, 0.3, 0.3, 0.1);
            world.spawnParticle(Particle.ELECTRIC_SPARK, center, 25, 0.4, 0.4, 0.4, 0.15);
        } else {
            // Smoke puff when turning off
            world.spawnParticle(Particle.SMOKE, center, 15, 0.3, 0.3, 0.3, 0.05);
            world.spawnParticle(Particle.DUST, center, 10, 0.3, 0.3, 0.3, 0, SYNC_OFF_DUST);
        }
    }

    /**
     * Spawns particles when a block is connected to a group using the connector tool.
     * Green particles with upward sparks indicate successful connection.
     */
    public static void spawnConnectParticles(Location location) {
        World world = location.getWorld();
        if (world == null) return;

        Location center = location.clone().add(0.5, 0.5, 0.5);

        // Green dust particles swirling inward
        world.spawnParticle(Particle.DUST, center, 25, 0.4, 0.4, 0.4, 0, CONNECT_DUST);
        // Upward sparks indicating connection
        world.spawnParticle(Particle.HAPPY_VILLAGER, center, 8, 0.3, 0.3, 0.3, 0);
        // Electric sparks for the "link" effect
        world.spawnParticle(Particle.ELECTRIC_SPARK, center, 15, 0.3, 0.3, 0.3, 0.08);
    }

    /**
     * Spawns particles when a block is disconnected from a group using the connector tool.
     * Red particles with downward smoke indicate disconnection.
     */
    public static void spawnDisconnectParticles(Location location) {
        World world = location.getWorld();
        if (world == null) return;

        Location center = location.clone().add(0.5, 0.5, 0.5);

        // Red dust particles dispersing outward
        world.spawnParticle(Particle.DUST, center, 20, 0.5, 0.5, 0.5, 0, DISCONNECT_DUST);
        // Smoke puff for the "unlink" effect
        world.spawnParticle(Particle.SMOKE, center, 12, 0.3, 0.3, 0.3, 0.03);
        // Small crit particles for visual feedback
        world.spawnParticle(Particle.CRIT, center, 10, 0.3, 0.3, 0.3, 0.1);
    }

    /**
     * Draws a private particle line for debug viewers between two linked wireless blocks.
     */
    public static void spawnDebugConnectionLine(Player player, Location source, Location target, Color color) {
        if (source.getWorld() == null || target.getWorld() == null) return;
        if (!source.getWorld().equals(target.getWorld())) return;
        if (!player.getWorld().equals(source.getWorld())) return;

        Location start = source.clone().add(0.5, 0.5, 0.5);
        Location end = target.clone().add(0.5, 0.5, 0.5);
        Vector direction = end.toVector().subtract(start.toVector());
        double distance = direction.length();
        if (distance <= 0.01) return;

        direction.normalize();
        int steps = Math.min(220, Math.max(2, (int) Math.ceil(distance * 2.0)));
        double stepLength = distance / steps;
        Particle.DustOptions dust = new Particle.DustOptions(color, 0.9f);

        for (int i = 0; i <= steps; i++) {
            Location point = start.clone().add(direction.clone().multiply(stepLength * i));
            player.spawnParticle(Particle.DUST, point, 1, 0, 0, 0, 0, dust);
        }
    }
}
