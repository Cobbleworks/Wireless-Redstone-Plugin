package com.wirelessredstone.model;

import com.wirelessredstone.item.BulbVariant;
import org.bukkit.Location;

import java.util.Optional;
import java.util.UUID;

public class BulbPair {

    private final UUID pairId;
    private Location location1;
    private Location location2;
    private boolean lit;
    private UUID ownerUuid;
    private BulbVariant.BulbType bulbType;

    public BulbPair(UUID pairId) {
        this.pairId = pairId;
        this.lit = false;
        this.bulbType = BulbVariant.BulbType.COPPER_BULB;
    }

    public BulbPair(UUID pairId, UUID ownerUuid, BulbVariant.BulbType bulbType) {
        this.pairId = pairId;
        this.lit = false;
        this.ownerUuid = ownerUuid;
        this.bulbType = bulbType;
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

    public UUID getOwnerUuid() {
        return ownerUuid;
    }

    public void setOwnerUuid(UUID ownerUuid) {
        this.ownerUuid = ownerUuid;
    }

    public BulbVariant.BulbType getBulbType() {
        return bulbType;
    }

    public void setBulbType(BulbVariant.BulbType bulbType) {
        this.bulbType = bulbType;
    }

    public boolean isComplete() {
        return location1 != null && location2 != null;
    }
}
