package org.sosly.villageworks.capability.villages;

import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import org.sosly.villageworks.api.capability.IVillagesCapability;
import org.sosly.villageworks.data.VillageData;

import java.lang.ref.WeakReference;
import java.util.*;

public class VillagesCapability implements IVillagesCapability {

    private final Map<UUID, VillageData> villages;
    private final Map<String, UUID> villagesByName;
    private WeakReference<Level> ownerLevel;

    public VillagesCapability() {
        this.villages = new HashMap<>();
        this.villagesByName = new HashMap<>();
        this.ownerLevel = new WeakReference<>(null);
    }

    @Override
    public VillageData getVillageAt(ChunkPos pos) {
        if (pos == null) {
            return null;
        }

        for (VillageData village : villages.values()) {
            if (village.containsChunk(pos)) {
                return village;
            }
        }
        return null;
    }

    @Override
    public UUID createVillage(ChunkPos townHallPos, String villageName, int squadius) {
        if (townHallPos == null || villageName == null || villageName.trim().isEmpty()) {
            return null;
        }

        if (squadius < 1) {
            return null;
        }

        if (villagesByName.containsKey(villageName)) {
            return null;
        }

        UUID villageId = UUID.randomUUID();
        VillageData newVillage = new VillageData(villageId, townHallPos, villageName, squadius);

        if (!canClaimChunk(townHallPos, null)) {
            return null;
        }

        for (VillageData existingVillage : villages.values()) {
            if (existingVillage.overlaps(newVillage)) {
                return null;
            }
        }

        villages.put(villageId, newVillage);
        villagesByName.put(villageName, villageId);
        markDirty();

        return villageId;
    }

    @Override
    public boolean removeVillage(UUID villageId) {
        if (villageId == null) {
            return false;
        }

        VillageData village = villages.remove(villageId);
        if (village == null) {
            return false;
        }

        villagesByName.remove(village.getVillageName());
        markDirty();
        return true;
    }

    @Override
    public boolean canClaimChunk(ChunkPos pos, UUID excludeVillageId) {
        if (pos == null) {
            return false;
        }

        for (VillageData village : villages.values()) {
            if (excludeVillageId != null && village.getVillageId().equals(excludeVillageId)) {
                continue;
            }

            if (village.containsChunk(pos)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public Collection<VillageData> getVillages() {
        return new ArrayList<>(villages.values());
    }

    @Override
    public VillageData getVillageById(UUID villageId) {
        if (villageId == null) {
            return null;
        }
        return villages.get(villageId);
    }

    @Override
    public VillageData getVillageByName(String villageName) {
        if (villageName == null || villageName.trim().isEmpty()) {
            return null;
        }

        UUID villageId = villagesByName.get(villageName);
        if (villageId == null) {
            return null;
        }

        return villages.get(villageId);
    }

    public void setOwnerLevel(Level level) {
        this.ownerLevel = new WeakReference<>(level);
    }

    private void markDirty() {
        // Level capabilities are automatically saved
    }

    public void loadVillage(VillageData village) {
        if (village == null) {
            return;
        }

        villages.put(village.getVillageId(), village);
        villagesByName.put(village.getVillageName(), village.getVillageId());
    }
}
