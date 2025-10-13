package org.sosly.villagetale.helper;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.AABB;
import org.sosly.villagetale.data.VillageInfo;

public class ZoneValidationHelper {

    public static boolean isPositionWithinBoundary(VillageInfo village, BlockPos pos) {
        if (village == null || pos == null) {
            return false;
        }

        ChunkPos chunk = new ChunkPos(pos);
        return village.containsChunk(chunk);
    }

    public static boolean isBoxWithinBoundary(VillageInfo village, AABB bounds) {
        if (village == null || bounds == null) {
            return false;
        }

        BlockPos min = new BlockPos((int) bounds.minX, (int) bounds.minY, (int) bounds.minZ);
        BlockPos max = new BlockPos((int) bounds.maxX - 1, (int) bounds.maxY - 1, (int) bounds.maxZ - 1);

        if (!isPositionWithinBoundary(village, min)) {
            return false;
        }

        if (!isPositionWithinBoundary(village, max)) {
            return false;
        }

        ChunkPos minChunk = new ChunkPos(min);
        ChunkPos maxChunk = new ChunkPos(max);

        for (int x = minChunk.x; x <= maxChunk.x; x++) {
            for (int z = minChunk.z; z <= maxChunk.z; z++) {
                if (!village.containsChunk(new ChunkPos(x, z))) {
                    return false;
                }
            }
        }

        return true;
    }

    public static boolean isCylinderWithinBoundary(VillageInfo village, BlockPos center, int radius) {
        if (village == null || center == null) {
            return false;
        }

        BlockPos minCorner = center.offset(-radius, 0, -radius);
        BlockPos maxCorner = center.offset(radius, 0, radius);

        ChunkPos minChunk = new ChunkPos(minCorner);
        ChunkPos maxChunk = new ChunkPos(maxCorner);

        for (int x = minChunk.x; x <= maxChunk.x; x++) {
            for (int z = minChunk.z; z <= maxChunk.z; z++) {
                if (!village.containsChunk(new ChunkPos(x, z))) {
                    return false;
                }
            }
        }

        return true;
    }
}
