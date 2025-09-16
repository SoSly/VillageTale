package org.sosly.villageworks.api.capability;

import net.minecraft.world.level.ChunkPos;
import org.sosly.villageworks.data.VillageData;

import java.util.Collection;
import java.util.UUID;

public interface IVillagesCapability {

    VillageData getVillageAt(ChunkPos pos);

    UUID createVillage(ChunkPos townHallPos, String villageName, int squadius);

    boolean removeVillage(UUID villageId);

    boolean canClaimChunk(ChunkPos pos, UUID excludeVillageId);

    Collection<VillageData> getVillages();

    VillageData getVillageById(UUID villageId);

    VillageData getVillageByName(String villageName);
}
