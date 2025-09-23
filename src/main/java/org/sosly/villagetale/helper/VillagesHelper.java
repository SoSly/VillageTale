package org.sosly.villagetale.helper;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import org.sosly.villagetale.api.IVillageZone;
import org.sosly.villagetale.api.capability.IVillageCapability;
import org.sosly.villagetale.api.capability.IVillagesCapability;
import org.sosly.villagetale.capability.Capabilities;
import org.sosly.villagetale.config.CommonConfig;
import org.sosly.villagetale.data.VillageInfo;
import org.sosly.villagetale.entity.MemoryModuleTypes;
import org.sosly.villagetale.entity.Villager;

public class VillagesHelper {
    public static IVillageCapability getVillageCapability(ServerLevel level, UUID uuid) {
        IVillagesCapability villages = level.getCapability(Capabilities.VILLAGES_CAPABILITY).orElse(null);
        if (villages == null) {
            return null;
        }

        VillageInfo info = villages.getVillageById(uuid);
        if (info == null) {
            return null;
        }

        ChunkPos startingChunk = info.getVillageStartingChunk();
        LevelChunk chunk = level.getChunk(startingChunk.x, startingChunk.z);

        IVillageCapability village = chunk.getCapability(Capabilities.VILLAGE_CAPABILITY).orElse(null);
        if (village == null) {
            return null;
        }
        return village;
    }

    public static IVillageZone getZoneById(ServerLevel level, UUID villageId, UUID zoneId) {
        IVillageCapability village = VillagesHelper.getVillageCapability(level, villageId);
        if (village == null) {
            return null;
        }

        return village.getZones().stream()
                .filter(z -> z.getUUID().equals(zoneId))
                .findFirst()
                .orElse(null);
    }



    public static IVillageZone getWorkplaceZone(ServerLevel level, Villager villager) {
        UUID villageId = villager.getVillage().get();
        UUID workplaceId = villager.getBrain().getMemory(MemoryModuleTypes.WORK_ZONE.get()).orElse(null);
        if (workplaceId == null) {
            return null;
        }

        return getZoneById(level, villageId, workplaceId);
    }

    public static IVillageZone getZoneAtPosition(ServerLevel level, UUID villageId, BlockPos position, ResourceLocation zoneType) {
        IVillageCapability village = getVillageCapability(level, villageId);
        if (village == null) {
            return null;
        }

        return village.getZones()
                .stream()
                .filter(z -> z.getType().getID().equals(zoneType))
                .filter(z -> position.closerThan(z.getStartPosition().atY(position.getY()), CommonConfig.interactionDistance))
                .findFirst()
                .orElse(null);
    }
}
