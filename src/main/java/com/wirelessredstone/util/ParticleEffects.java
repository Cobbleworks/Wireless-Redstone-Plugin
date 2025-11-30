package com.wirelessredstone.util;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;

public class ParticleEffects {

    private static final Particle.DustOptions SYNC_ON_DUST = new Particle.DustOptions(Color.fromRGB(0, 255, 200), 1.0f);
    private static final Particle.DustOptions SYNC_OFF_DUST = new Particle.DustOptions(Color.fromRGB(255, 100, 50), 1.0f);
    private static final Particle.DustOptions BREAK_DUST = new Particle.DustOptions(Color.fromRGB(255, 0, 0), 1.5f);

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
        world.spawnParticle(Particle.FLASH, center, 1, 0, 0, 0, 0);
    }
}
