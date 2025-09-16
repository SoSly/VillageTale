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
    private final BlockPos townHallPos;
    private final ChunkPos villageStartingChunk;
    private final List<IVillageZone> zones;
    private final Set<UUID> villagerIds;
    private final Map<UUID, Permission> playerPermissions;
    private WeakReference<LevelChunk> ownerChunk;
    
    public VillageCapability(UUID villageId, BlockPos townHallPos, ChunkPos villageStartingChunk) {
        this.villageId = villageId;
        this.townHallPos = townHallPos;
        this.villageStartingChunk = villageStartingChunk;
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
    public BlockPos getTownHallPos() {
        return townHallPos;
    }
    
    @Override
    public ChunkPos getVillageStartingChunk() {
        return villageStartingChunk;
    }
    
    @Override
    public List<IVillageZone> getZones() {
        return new ModifiableZoneList(zones, this::markDirty);
    }
    
    private static class ModifiableZoneList extends ArrayList<IVillageZone> {
        private final Runnable onModify;
        
        public ModifiableZoneList(List<IVillageZone> zones, Runnable onModify) {
            super(zones);
            this.onModify = onModify;
        }
        
        @Override
        public IVillageZone set(int index, IVillageZone element) {
            IVillageZone result = super.set(index, element);
            onModify.run();
            return result;
        }
        
        @Override
        public boolean add(IVillageZone zone) {
            boolean result = super.add(zone);
            if (result) onModify.run();
            return result;
        }
        
        @Override
        public void add(int index, IVillageZone element) {
            super.add(index, element);
            onModify.run();
        }
        
        @Override
        public IVillageZone remove(int index) {
            IVillageZone result = super.remove(index);
            onModify.run();
            return result;
        }
        
        @Override
        public boolean remove(Object o) {
            boolean result = super.remove(o);
            if (result) onModify.run();
            return result;
        }
    }
    
    @Override
    public void addZone(IVillageZone zone) {
        if (zone == null) {
            return;
        }
        
        zones.add(zone);
        markDirty();
    }
    
    public void addZoneWithoutDirty(IVillageZone zone) {
        if (zone == null) {
            return;
        }
        
        zones.add(zone);
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
    
    public void markDirty() {
        LevelChunk chunk = ownerChunk.get();
        if (chunk != null) {
            chunk.setUnsaved(true);
        }
    }
}