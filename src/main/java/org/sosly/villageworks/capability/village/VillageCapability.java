package org.sosly.villageworks.capability.village;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.chunk.LevelChunk;
import org.sosly.villageworks.api.capability.IVillageCapability;
import org.sosly.villageworks.api.data.IVillageZone;
import org.sosly.villageworks.api.data.ZoneType;
import org.sosly.villageworks.data.zones.AbstractVillageZone;

public class VillageCapability implements IVillageCapability {

    @Nullable
    private UUID villageId;
    private Map<UUID, IVillageZone> zones;
    private Set<UUID> villagerIds;
    private Map<UUID, Permission> playerPermissions;
    private Map<ZoneType, Integer> zoneCounters;
    private WeakReference<LevelChunk> ownerChunk;

    public VillageCapability() {
        this.villageId = null;
        this.zones = null;
        this.villagerIds = null;
        this.playerPermissions = null;
        this.zoneCounters = null;
        this.ownerChunk = new WeakReference<>(null);
    }

    @Override
    public UUID getVillageId() {
        return villageId;
    }

    @Override
    public List<IVillageZone> getZones() {
        if (zones == null) {
            return Collections.emptyList();
        }
        return new ArrayList<>(zones.values());
    }

    @Override
    public void addZone(IVillageZone zone) {
        if (zone == null || villageId == null) {
            return;
        }

        ensureCollectionsInitialized();
        int zoneId = getNextZoneId(zone.getType());
        if (zone instanceof AbstractVillageZone) {
            ((AbstractVillageZone) zone).setId(zoneId);
        }
        zones.put(zone.getUUID(), zone);
        markDirty();
    }

    public void addExistingZone(IVillageZone zone) {
        if (zone == null || villageId == null) {
            return;
        }

        ensureCollectionsInitialized();
        zones.put(zone.getUUID(), zone);
        updateZoneCounterFromExisting(zone);
    }

    @Override
    public boolean removeZone(UUID zoneId) {
        if (zoneId == null || zones == null) {
            return false;
        }

        boolean removed = zones.remove(zoneId) != null;
        if (removed) {
            markDirty();
        }
        return removed;
    }

    @Override
    public IVillageZone getZoneAt(BlockPos pos) {
        if (pos == null || zones == null) {
            return null;
        }

        for (IVillageZone zone : zones.values()) {
            if (zone.containsPosition(pos)) {
                return zone;
            }
        }
        return null;
    }

    @Override
    public Set<UUID> getVillagerIds() {
        if (villagerIds == null) {
            return Collections.emptySet();
        }
        return new HashSet<>(villagerIds);
    }

    @Override
    public void assignVillager(UUID villagerId) {
        if (villagerId == null || villageId == null) {
            return;
        }

        ensureCollectionsInitialized();
        if (villagerIds.add(villagerId)) {
            markDirty();
        }
    }

    @Override
    public boolean removeVillager(UUID villagerId) {
        if (villagerId == null || villagerIds == null) {
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
        if (playerPermissions == null) {
            return Collections.emptyMap();
        }
        return new HashMap<>(playerPermissions);
    }

    @Override
    public void setPlayerPermission(UUID playerId, Permission permission) {
        if (playerId == null || permission == null || villageId == null) {
            return;
        }

        ensureCollectionsInitialized();
        
        if (permission == Permission.NONE) {
            if (playerPermissions.remove(playerId) != null) {
                markDirty();
            }
            return;
        }
        
        Permission oldPermission = playerPermissions.get(playerId);
        playerPermissions.put(playerId, permission);
        if (!permission.equals(oldPermission)) {
            markDirty();
        }
    }

    @Override
    public boolean hasPermission(UUID playerId, Permission required) {
        if (playerId == null || required == null || playerPermissions == null) {
            return false;
        }

        Permission playerPermission = playerPermissions.getOrDefault(playerId, Permission.NONE);
        return playerPermission.ordinal() >= required.ordinal();
    }

    public void initializeVillage(UUID villageId) {
        this.villageId = villageId;
        ensureCollectionsInitialized();
        markDirty();
    }

    public boolean hasVillage() {
        return villageId != null;
    }

    public void setOwnerChunk(LevelChunk chunk) {
        this.ownerChunk = new WeakReference<>(chunk);
    }

    @Override
    public boolean assignVillagerToZone(UUID zoneId, UUID villagerUUID) {
        if (zoneId == null || villagerUUID == null || zones == null) {
            return false;
        }

        IVillageZone zone = zones.get(zoneId);
        if (zone == null) {
            return false;
        }

        zone.addAssignedVillager(villagerUUID);
        markDirty();
        return true;
    }

    @Override
    public boolean unassignVillagerFromZone(UUID zoneId, UUID villagerUUID) {
        if (zoneId == null || villagerUUID == null || zones == null) {
            return false;
        }

        IVillageZone zone = zones.get(zoneId);
        if (zone == null) {
            return false;
        }

        boolean removed = zone.removeAssignedVillager(villagerUUID);
        if (removed) {
            markDirty();
        }
        return removed;
    }

    @Override
    public boolean claimPositionInZone(UUID zoneId, BlockPos pos, UUID villagerUUID, int durationTicks, long currentTime) {
        if (zoneId == null || pos == null || villagerUUID == null || zones == null) {
            return false;
        }

        IVillageZone zone = zones.get(zoneId);
        if (zone == null) {
            return false;
        }

        boolean claimed = zone.claim(pos, villagerUUID, durationTicks, currentTime);
        if (claimed) {
            markDirty();
        }
        return claimed;
    }

    @Override
    public boolean releasePositionInZone(UUID zoneId, BlockPos pos) {
        if (zoneId == null || pos == null || zones == null) {
            return false;
        }

        IVillageZone zone = zones.get(zoneId);
        if (zone == null) {
            return false;
        }

        boolean released = zone.release(pos);
        if (released) {
            markDirty();
        }
        return released;
    }

    public void markDirty() {
        LevelChunk chunk = ownerChunk.get();
        if (chunk != null) {
            chunk.setUnsaved(true);
        }
    }

    private void ensureCollectionsInitialized() {
        if (zones == null) {
            zones = new HashMap<>();
        }
        if (villagerIds == null) {
            villagerIds = new HashSet<>();
        }
        if (playerPermissions == null) {
            playerPermissions = new HashMap<>();
        }
        if (zoneCounters == null) {
            zoneCounters = new HashMap<>();
        }
    }

    private int getNextZoneId(ZoneType type) {
        ensureCollectionsInitialized();
        int nextId = zoneCounters.getOrDefault(type, 0) + 1;
        zoneCounters.put(type, nextId);
        return nextId;
    }

    private void updateZoneCounterFromExisting(IVillageZone zone) {
        ensureCollectionsInitialized();
        ZoneType type = zone.getType();
        int zoneId = ((AbstractVillageZone) zone).getId();
        int currentMax = zoneCounters.getOrDefault(type, 0);
        if (zoneId > currentMax) {
            zoneCounters.put(type, zoneId);
        }
    }
}
