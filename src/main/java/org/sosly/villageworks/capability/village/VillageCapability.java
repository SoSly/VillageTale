package org.sosly.villageworks.capability.village;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import org.sosly.villageworks.api.capability.IVillageCapability;
import org.sosly.villageworks.api.data.IVillageZone;

import java.lang.ref.WeakReference;
import java.util.*;

public class VillageCapability implements IVillageCapability {
    
    private final UUID villageId;
    private final ChunkPos townHallPos;
    private final List<IVillageZone> zones;
    private final Set<UUID> villagerIds;
    private final Map<UUID, Permission> playerPermissions;
    private WeakReference<LevelChunk> ownerChunk;
    
    public VillageCapability(UUID villageId, ChunkPos townHallPos) {
        this.villageId = villageId;
        this.townHallPos = townHallPos;
        this.zones = new ArrayList<>();
        this.villagerIds = new HashSet<>();
        this.playerPermissions = new HashMap<>();
        this.ownerChunk = new WeakReference<>(null);
    }
    
    @Override
    public UUID getVillageId() {
        return villageId;
    }
    
    @Override
    public ChunkPos getTownHallPos() {
        return townHallPos;
    }
    
    @Override
    public List<IVillageZone> getZones() {
        return new ArrayList<>(zones);
    }
    
    @Override
    public void addZone(IVillageZone zone) {
        if (zone == null) {
            return;
        }
        
        zones.add(zone);
        markDirty();
    }
    
    @Override
    public boolean removeZone(UUID zoneId) {
        if (zoneId == null) {
            return false;
        }
        
        boolean removed = zones.removeIf(zone -> zoneId.equals(zone.getUUID()));
        if (removed) {
            markDirty();
        }
        return removed;
    }
    
    @Override
    public IVillageZone getZoneAt(BlockPos pos) {
        if (pos == null) {
            return null;
        }
        
        for (IVillageZone zone : zones) {
            if (zone.containsPosition(pos)) {
                return zone;
            }
        }
        return null;
    }
    
    @Override
    public Set<UUID> getVillagerIds() {
        return new HashSet<>(villagerIds);
    }
    
    @Override
    public void assignVillager(UUID villagerId) {
        if (villagerId == null) {
            return;
        }
        
        if (villagerIds.add(villagerId)) {
            markDirty();
        }
    }
    
    @Override
    public boolean removeVillager(UUID villagerId) {
        if (villagerId == null) {
            return false;
        }
        
        boolean removed = villagerIds.remove(villagerId);
        if (removed) {
            markDirty();
        }
        return removed;
    }
    
    @Override
    public Map<UUID, Permission> getPlayerPermissions() {
        return new HashMap<>(playerPermissions);
    }
    
    @Override
    public void setPlayerPermission(UUID playerId, Permission permission) {
        if (playerId == null || permission == null) {
            return;
        }
        
        if (permission == Permission.NONE) {
            if (playerPermissions.remove(playerId) != null) {
                markDirty();
            }
        } else {
            Permission oldPermission = playerPermissions.put(playerId, permission);
            if (!permission.equals(oldPermission)) {
                markDirty();
            }
        }
    }
    
    @Override
    public boolean hasPermission(UUID playerId, Permission required) {
        if (playerId == null || required == null) {
            return false;
        }
        
        Permission playerPermission = playerPermissions.getOrDefault(playerId, Permission.NONE);
        return playerPermission.ordinal() >= required.ordinal();
    }
    
    public void setOwnerChunk(LevelChunk chunk) {
        this.ownerChunk = new WeakReference<>(chunk);
    }
    
    private void markDirty() {
        LevelChunk chunk = ownerChunk.get();
        if (chunk != null) {
            chunk.setUnsaved(true);
        }
    }
}