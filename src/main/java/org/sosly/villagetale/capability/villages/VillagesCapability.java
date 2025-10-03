package org.sosly.villagetale.capability.villages;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.PacketDistributor;
import org.sosly.villagetale.api.capability.IVillagesCapability;
import org.sosly.villagetale.data.VillageInfo;
import org.sosly.villagetale.network.NetworkHandler;
import org.sosly.villagetale.network.packets.clientbound.VillageBoundaryPacket;

public class VillagesCapability implements IVillagesCapability {

    private final Map<UUID, VillageInfo> villages;
    private final Map<String, UUID> villagesByName;
    private final Level level;

    public VillagesCapability(Level level) {
        this.villages = new HashMap<>();
        this.villagesByName = new HashMap<>();
        this.level = level;
    }

    @Override
    public VillageInfo getVillageAt(ChunkPos pos) {
        if (pos == null) {
            return null;
        }

        for (VillageInfo village : villages.values()) {
            if (village.containsChunk(pos)) {
                return village;
            }
        }

        return null;
    }

    @Override
    public UUID createVillage(BlockPos townHallPos, String villageName, int squadius) {
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
        ChunkPos villageStartingChunk = new ChunkPos(townHallPos);
        VillageInfo newVillage = new VillageInfo(villageId, townHallPos, villageStartingChunk, villageName, squadius);

        int minDistance = 30;
        for (VillageInfo existingVillage : villages.values()) {
            if (existingVillage.overlaps(newVillage, minDistance)) {
                return null;
            }
        }

        villages.put(villageId, newVillage);
        villagesByName.put(villageName, villageId);

        if (level.isClientSide) {
            return villageId;
        }

        VillageBoundaryPacket packet = new VillageBoundaryPacket(
            villageId,
            villageStartingChunk,
            squadius
        );
        NetworkHandler.CHANNEL.send(
            PacketDistributor.DIMENSION.with(() -> level.dimension()),
            packet
        );

        return villageId;
    }

    @Override
    public boolean removeVillage(UUID villageId) {
        if (villageId == null) {
            return false;
        }

        VillageInfo village = villages.remove(villageId);
        if (village == null) {
            return false;
        }

        villagesByName.remove(village.getVillageName());
        return true;
    }

    @Override
    public boolean canClaimChunk(ChunkPos pos, UUID excludeVillageId) {
        if (pos == null) {
            return false;
        }

        for (VillageInfo village : villages.values()) {
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
    public Collection<VillageInfo> getVillages() {
        return new ArrayList<>(villages.values());
    }

    @Override
    public VillageInfo getVillageById(UUID villageId) {
        if (villageId == null) {
            return null;
        }
        return villages.get(villageId);
    }

    @Override
    public VillageInfo getVillageByName(String villageName) {
        if (villageName == null || villageName.trim().isEmpty()) {
            return null;
        }

        UUID villageId = villagesByName.get(villageName);
        if (villageId == null) {
            return null;
        }

        return villages.get(villageId);
    }

    @Override
    public boolean updateTownHallPos(UUID villageId, BlockPos newPos) {
        if (villageId == null) {
            return false;
        }

        VillageInfo village = villages.get(villageId);
        if (village == null) {
            return false;
        }

        village.setTownHallPos(newPos);
        return true;
    }

    public void loadVillage(VillageInfo village) {
        if (village == null) {
            return;
        }

        villages.put(village.getVillageId(), village);
        villagesByName.put(village.getVillageName(), village.getVillageId());
    }
}
