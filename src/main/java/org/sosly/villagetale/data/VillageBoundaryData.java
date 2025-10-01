package org.sosly.villagetale.data;

import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.AABB;

import java.util.UUID;

public class VillageBoundaryData {
    private final UUID villageId;
    private final ChunkPos centerChunk;
    private final int squadius;

    public VillageBoundaryData(UUID villageId, ChunkPos centerChunk, int squadius) {
        this.villageId = villageId;
        this.centerChunk = centerChunk;
        this.squadius = squadius;
    }

    public UUID getVillageId() {
        return villageId;
    }

    public ChunkPos getCenterChunk() {
        return centerChunk;
    }

    public int getSquadius() {
        return squadius;
    }

    public AABB getAABB() {
        int minX = (centerChunk.x - squadius) << 4;
        int maxX = ((centerChunk.x + squadius + 1) << 4);
        int minZ = (centerChunk.z - squadius) << 4;
        int maxZ = ((centerChunk.z + squadius + 1) << 4);
        return new AABB(minX, -64, minZ, maxX, 320, maxZ);
    }
}
