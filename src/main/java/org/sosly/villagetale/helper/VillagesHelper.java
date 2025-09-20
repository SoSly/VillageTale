package org.sosly.villagetale.helper;

import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import org.sosly.villagetale.api.IVillageZone;
import org.sosly.villagetale.api.capability.IVillageCapability;
import org.sosly.villagetale.api.capability.IVillagesCapability;
import org.sosly.villagetale.capability.Capabilities;
import org.sosly.villagetale.data.VillageInfo;

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

        ChunkPos townHallChunk = new ChunkPos(info.getTownHallPos());
        LevelChunk chunk = level.getChunk(townHallChunk.x, townHallChunk.z);

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
}
