package org.sosly.villagetale.data.zones;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import org.sosly.villagetale.api.data.ZoneShape;
import org.sosly.villagetale.api.data.ZoneType;

public class RadiusVillageZone extends AbstractVillageZone {

    private BlockPos center;
    private int radius;
    private List<BlockPos> cachedPOIs;
    private boolean poiCacheDirty = true;

    public RadiusVillageZone(UUID uuid, ZoneType type, String name, BlockPos center, int radius, Level level) {
        super(uuid, type, name, level);
        this.center = center;
        this.radius = radius;
    }

    public RadiusVillageZone(UUID uuid, ZoneType type, String name, BlockPos center, int radius) {
        super(uuid, type, name);
        this.center = center;
        this.radius = radius;
    }

    public RadiusVillageZone(UUID uuid, ZoneType type, int id, String name, BlockPos center, int radius, Level level) {
        super(uuid, type, id, name, level);
        this.center = center;
        this.radius = radius;
    }

    public RadiusVillageZone(UUID uuid, ZoneType type, int id, String name, BlockPos center, int radius) {
        super(uuid, type, id, name);
        this.center = center;
        this.radius = radius;
    }

    public BlockPos getCenter() {
        return center;
    }

    public void setCenter(BlockPos center) {
        this.center = center;
        this.poiCacheDirty = true;
    }

    public int getRadius() {
        return radius;
    }

    public void setRadius(int radius) {
        this.radius = radius;
        this.poiCacheDirty = true;
    }

    @Override
    public ZoneShape getShape() {
        return ZoneShape.RADIUS;
    }

    @Override
    public boolean containsPosition(BlockPos pos) {
        if (center == null || pos == null) {
            return false;
        }

        double distanceSquared = center.distSqr(pos);
        return distanceSquared <= radius * radius;
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
        if (level == null || getType() == ZoneType.NONE || center == null) {
            cachedPOIs = new ArrayList<>();
            poiCacheDirty = false;
            return;
        }

        cachedPOIs = new ArrayList<>();

        int minX = center.getX() - radius;
        int maxX = center.getX() + radius;
        int minY = Math.max(level.getMinBuildHeight(), center.getY() - radius);
        int maxY = Math.min(level.getMaxBuildHeight(), center.getY() + radius);
        int minZ = center.getZ() - radius;
        int maxZ = center.getZ() + radius;

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (containsPosition(pos) && getType().isPOI(pos, level)) {
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
        tag.putByte("Shape", (byte) ZoneShape.RADIUS.ordinal());

        if (center != null) {
            tag.putLong("Center", center.asLong());
        }
        tag.putInt("Radius", radius);

        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        super.deserializeNBT(tag);

        if (tag.contains("Center")) {
            this.center = BlockPos.of(tag.getLong("Center"));
        }
        this.radius = tag.getInt("Radius");
        this.poiCacheDirty = true;
    }
}
