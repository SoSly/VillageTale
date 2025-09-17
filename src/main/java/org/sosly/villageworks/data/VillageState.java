package org.sosly.villageworks.data;

import org.sosly.villageworks.api.capability.IVillageCapability;
import org.sosly.villageworks.api.data.IVillageZone;
import org.sosly.villageworks.api.data.ZoneType;
import org.sosly.villageworks.data.zones.AbstractVillageZone;
import net.minecraft.nbt.CompoundTag;

import java.util.*;

public class VillageState {
    
    private final UUID villageId;
    private final List<IVillageZone> zones;
    private final Set<UUID> villagerIds;
    private final Map<UUID, IVillageCapability.Permission> playerPermissions;
    private final Map<ZoneType, Integer> zoneCounters;
    
    public VillageState(UUID villageId) {
        this.villageId = villageId;
        this.zones = new ArrayList<>();
        this.villagerIds = new HashSet<>();
        this.playerPermissions = new HashMap<>();
        this.zoneCounters = new HashMap<>();
    }
    
    public UUID getVillageId() {
        return villageId;
    }
    
    public List<IVillageZone> getZones() {
        return zones;
    }
    
    public Set<UUID> getVillagerIds() {
        return villagerIds;
    }
    
    public Map<UUID, IVillageCapability.Permission> getPlayerPermissions() {
        return playerPermissions;
    }
    
    public Map<ZoneType, Integer> getZoneCounters() {
        return zoneCounters;
    }
    
    public void addZone(IVillageZone zone) {
        if (zone != null) {
            int zoneId = getNextZoneId(zone.getType());
            if (zone instanceof AbstractVillageZone) {
                ((AbstractVillageZone) zone).setId(zoneId);
            }
            zones.add(zone);
        }
    }
    
    public void addExistingZone(IVillageZone zone) {
        if (zone != null) {
            zones.add(zone);
            updateZoneCounterFromExisting(zone);
        }
    }
    
    public boolean removeZone(IVillageZone zone) {
        return zones.remove(zone);
    }
    
    public int getNextZoneId(ZoneType type) {
        int nextId = zoneCounters.getOrDefault(type, 0) + 1;
        zoneCounters.put(type, nextId);
        return nextId;
    }
    
    public void updateZoneCounterFromExisting(IVillageZone zone) {
        ZoneType type = zone.getType();
        int zoneId = ((AbstractVillageZone) zone).getId();
        int currentMax = zoneCounters.getOrDefault(type, 0);
        if (zoneId > currentMax) {
            zoneCounters.put(type, zoneId);
        }
    }
    
    public void addVillager(UUID villagerId) {
        if (villagerId != null) {
            villagerIds.add(villagerId);
        }
    }
    
    public boolean removeVillager(UUID villagerId) {
        return villagerIds.remove(villagerId);
    }
    
    public void setPlayerPermission(UUID playerId, IVillageCapability.Permission permission) {
        if (playerId != null && permission != null) {
            playerPermissions.put(playerId, permission);
        }
    }
    
    public IVillageCapability.Permission getPlayerPermission(UUID playerId) {
        return playerPermissions.get(playerId);
    }
    
    public boolean removePlayerPermission(UUID playerId) {
        return playerPermissions.remove(playerId) != null;
    }
    
    public boolean isEmpty() {
        return zones.isEmpty() && villagerIds.isEmpty() && playerPermissions.isEmpty();
    }
}