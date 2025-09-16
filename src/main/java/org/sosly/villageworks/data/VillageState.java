package org.sosly.villageworks.data;

import org.sosly.villageworks.api.capability.IVillageCapability;
import org.sosly.villageworks.api.data.IVillageZone;

import java.util.*;

public class VillageState {
    
    private final UUID villageId;
    private final List<IVillageZone> zones;
    private final Set<UUID> villagerIds;
    private final Map<UUID, IVillageCapability.Permission> playerPermissions;
    
    public VillageState(UUID villageId) {
        this.villageId = villageId;
        this.zones = new ArrayList<>();
        this.villagerIds = new HashSet<>();
        this.playerPermissions = new HashMap<>();
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
    
    public void addZone(IVillageZone zone) {
        if (zone != null) {
            zones.add(zone);
        }
    }
    
    public boolean removeZone(IVillageZone zone) {
        return zones.remove(zone);
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