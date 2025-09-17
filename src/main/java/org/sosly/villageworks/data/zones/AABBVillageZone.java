package org.sosly.villageworks.data.zones;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.sosly.villageworks.api.data.IVillageZone;
import org.sosly.villageworks.api.data.ZoneShape;
import org.sosly.villageworks.api.data.ZoneType;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class AABBVillageZone extends AbstractVillageZone implements IVillageZone {

    private AABB bounds;
    private List<BlockPos> cachedPOIs;
    private boolean poiCacheDirty = true;

    public AABBVillageZone(UUID uuid, ZoneType type, String name, AABB bounds, Level level) {
        super(uuid, type, name, level);
        this.bounds = bounds;
    }

    public AABBVillageZone(UUID uuid, ZoneType type, String name, AABB bounds) {
        super(uuid, type, name);
        this.bounds = bounds;
    }
    
    public AABBVillageZone(UUID uuid, ZoneType type, int id, String name, AABB bounds, Level level) {
        super(uuid, type, id, name, level);
        this.bounds = bounds;
    }

    public AABBVillageZone(UUID uuid, ZoneType type, int id, String name, AABB bounds) {
        super(uuid, type, id, name);
        this.bounds = bounds;
    }


    public AABB getAABB() {
        return bounds;
    }

    public void setAABB(AABB bounds) {
        this.bounds = bounds;
        this.poiCacheDirty = true;
    }


    @Override
    public ZoneShape getShape() {
        return ZoneShape.AABB;
    }


    @Override
    public boolean containsPosition(BlockPos pos) {
        if (bounds == null || pos == null) {
            return false;
        }

        return pos.getX() >= bounds.minX && pos.getX() <= bounds.maxX &&
               pos.getY() >= bounds.minY && pos.getY() <= bounds.maxY &&
               pos.getZ() >= bounds.minZ && pos.getZ() <= bounds.maxZ;
    }

    @Override
    public Optional<List<BlockPos>> getPOIs() {
        if (getType() == ZoneType.NONE || getLevel() == null) {
            return Optional.empty();
        }

        if (poiCacheDirty) {
            rescanPOIs(getLevel());
        }

        return Optional.of(new ArrayList<>(cachedPOIs));
    }

    public void rescanPOIs(Level level) {
        if (level == null || getType() == ZoneType.NONE) {
            cachedPOIs = new ArrayList<>();
            poiCacheDirty = false;
            return;
        }

        cachedPOIs = new ArrayList<>();

        int minX = (int) Math.floor(bounds.minX);
        int maxX = (int) Math.ceil(bounds.maxX);
        int minY = (int) Math.floor(bounds.minY);
        int maxY = (int) Math.ceil(bounds.maxY);
        int minZ = (int) Math.floor(bounds.minZ);
        int maxZ = (int) Math.ceil(bounds.maxZ);

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (getType().isPOI(pos, level)) {
                        cachedPOIs.add(pos);
                    }
                }
            }
        }

        poiCacheDirty = false;
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = super.serializeNBT();
        tag.putByte("Shape", (byte) ZoneShape.AABB.ordinal());

        if (bounds != null) {
            CompoundTag boundsTag = new CompoundTag();
            boundsTag.putDouble("MinX", bounds.minX);
            boundsTag.putDouble("MinY", bounds.minY);
            boundsTag.putDouble("MinZ", bounds.minZ);
            boundsTag.putDouble("MaxX", bounds.maxX);
            boundsTag.putDouble("MaxY", bounds.maxY);
            boundsTag.putDouble("MaxZ", bounds.maxZ);
            tag.put("Bounds", boundsTag);
        }

        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        super.deserializeNBT(tag);

        if (tag.contains("Bounds")) {
            CompoundTag boundsTag = tag.getCompound("Bounds");
            this.bounds = new AABB(
                boundsTag.getDouble("MinX"),
                boundsTag.getDouble("MinY"),
                boundsTag.getDouble("MinZ"),
                boundsTag.getDouble("MaxX"),
                boundsTag.getDouble("MaxY"),
                boundsTag.getDouble("MaxZ")
            );
        }

        this.poiCacheDirty = true;
    }
}
