package org.sosly.villageworks.capability.village;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import org.sosly.villageworks.api.capability.IVillageCapability;
import org.sosly.villageworks.api.data.IVillageZone;
import org.sosly.villageworks.data.VillageState;

import java.lang.ref.WeakReference;
import java.util.*;
import javax.annotation.Nullable;

public class VillageCapability implements IVillageCapability {
    
    @Nullable
    private VillageState state;
    private WeakReference<LevelChunk> ownerChunk;
    
    public VillageCapability() {
        this.state = null;
        this.ownerChunk = new WeakReference<>(null);
    }
    
    @Override
    public UUID getVillageId() {
        return state != null ? state.getVillageId() : null;
    }
    
    @Override
    public List<IVillageZone> getZones() {
        if (state == null) {
            return Collections.emptyList();
        }
        return new ModifiableZoneList(state.getZones(), this::markDirty);
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
        if (zone == null || state == null) {
            return;
        }
        
        state.addZone(zone);
        markDirty();
    }
    
    public void addZoneWithoutDirty(IVillageZone zone) {
        if (zone == null || state == null) {
            return;
        }
        
        state.addZone(zone);
    }
    
    @Override
    public boolean removeZone(UUID zoneId) {
        if (zoneId == null || state == null) {
            return false;
        }
        
        boolean removed = state.getZones().removeIf(zone -> zoneId.equals(zone.getUUID()));
        if (removed) {
            markDirty();
        }
        return removed;
    }
    
    @Override
    public IVillageZone getZoneAt(BlockPos pos) {
        if (pos == null || state == null) {
            return null;
        }
        
        for (IVillageZone zone : state.getZones()) {
            if (zone.containsPosition(pos)) {
                return zone;
            }
        }
        return null;
    }
    
    @Override
    public Set<UUID> getVillagerIds() {
        if (state == null) {
            return Collections.emptySet();
        }
        return new HashSet<>(state.getVillagerIds());
    }
    
    @Override
    public void assignVillager(UUID villagerId) {
        if (villagerId == null || state == null) {
            return;
        }
        
        if (!state.getVillagerIds().contains(villagerId)) {
            state.addVillager(villagerId);
            markDirty();
        }
    }
    
    @Override
    public boolean removeVillager(UUID villagerId) {
        if (villagerId == null || state == null) {
            return false;
        }
        
        boolean removed = state.removeVillager(villagerId);
        if (removed) {
            markDirty();
        }
        return removed;
    }
    
    @Override
    public Map<UUID, Permission> getPlayerPermissions() {
        if (state == null) {
            return Collections.emptyMap();
        }
        return new HashMap<>(state.getPlayerPermissions());
    }
    
    @Override
    public void setPlayerPermission(UUID playerId, Permission permission) {
        if (playerId == null || permission == null || state == null) {
            return;
        }
        
        if (permission == Permission.NONE) {
            if (state.removePlayerPermission(playerId)) {
                markDirty();
            }
        } else {
            Permission oldPermission = state.getPlayerPermission(playerId);
            state.setPlayerPermission(playerId, permission);
            if (!permission.equals(oldPermission)) {
                markDirty();
            }
        }
    }
    
    @Override
    public boolean hasPermission(UUID playerId, Permission required) {
        if (playerId == null || required == null || state == null) {
            return false;
        }
        
        Permission playerPermission = state.getPlayerPermissions().getOrDefault(playerId, Permission.NONE);
        return playerPermission.ordinal() >= required.ordinal();
    }
    
    public void initializeVillage(UUID villageId) {
        this.state = new VillageState(villageId);
        markDirty();
    }
    
    public boolean hasVillage() {
        return state != null;
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