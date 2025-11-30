package com.wirelessredstone.model;

import org.bukkit.Location;

import java.util.Optional;
import java.util.UUID;

public class BulbPair {

    private final UUID pairId;
    private Location location1;
    private Location location2;
    private boolean lit;

    public BulbPair(UUID pairId) {
        this.pairId = pairId;
        this.lit = false;
    }

    public UUID getPairId() {
        return pairId;
    }

    public Location getLocation1() {
        return location1;
    }

    public Location getLocation2() {
        return location2;
    }

    public void setLocation(int index, Location location) {
        if (index == 0) {
            this.location1 = location;
        } else {
            this.location2 = location;
        }
    }

    public void removeLocation(Location location) {
        if (location.equals(location1)) {
            location1 = null;
        } else if (location.equals(location2)) {
            location2 = null;
        }
    }

    public Optional<Location> getOtherLocation(Location location) {
        if (location.equals(location1)) {
            return Optional.ofNullable(location2);
        } else if (location.equals(location2)) {
            return Optional.ofNullable(location1);
        }
        return Optional.empty();
    }

    public boolean isEmpty() {
        return location1 == null && location2 == null;
    }

    public boolean isLit() {
        return lit;
    }

    public void setLit(boolean lit) {
        this.lit = lit;
    }

    public boolean hasLocation(Location location) {
        return location.equals(location1) || location.equals(location2);
    }
}
