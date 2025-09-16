package org.sosly.villageworks.data.zones;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import org.sosly.villageworks.api.data.IVillageZone;
import org.sosly.villageworks.api.data.ZoneShape;
import org.sosly.villageworks.api.data.ZoneType;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class RadiusVillageZone implements IVillageZone {

    private final UUID uuid;
    private final ZoneType type;
    private final int id;
    private String name;
    private BlockPos center;
    private int radius;
    private List<BlockPos> cachedPOIs;
    private boolean poiCacheDirty = true;

    public RadiusVillageZone(UUID uuid, ZoneType type, int id, String name, BlockPos center, int radius) {
        this.uuid = Objects.requireNonNull(uuid, "UUID cannot be null");
        this.type = Objects.requireNonNull(type, "Zone type cannot be null");
        this.id = id;
        this.name = name;
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
    public UUID getUUID() {
        return uuid;
    }

    @Override
    public String getName() {
        if (name != null) {
            return name;
        }

        String translationKey = "villageworks.zone." + type.name().toLowerCase();
        String zoneName = Component.translatable(translationKey).getString();
        return Component.translatable("villageworks.zone.numbered", zoneName, id).getString();
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public ZoneShape getShape() {
        return ZoneShape.RADIUS;
    }

    @Override
    public ZoneType getType() {
        return type;
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
        if (getType() == ZoneType.NONE) {
            return Optional.empty();
        }

        if (poiCacheDirty) {
            rescanPOIs();
        }

        return Optional.of(new ArrayList<>(cachedPOIs));
    }

    private void rescanPOIs() {
        cachedPOIs = new ArrayList<>();

        if (center == null) {
            poiCacheDirty = false;
            return;
        }

        poiCacheDirty = false;
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
        CompoundTag tag = new CompoundTag();
        tag.putString("UUID", uuid.toString());
        tag.putByte("Type", (byte) type.ordinal());
        tag.putShort("Id", (short) id);
        if (name != null) {
            tag.putString("Name", name);
        }
        tag.putByte("Shape", (byte) ZoneShape.RADIUS.ordinal());

        if (center != null) {
            tag.putLong("Center", center.asLong());
        }
        tag.putInt("Radius", radius);

        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        if (tag.contains("Name")) {
            this.name = tag.getString("Name");
        }

        if (tag.contains("Center")) {
            this.center = BlockPos.of(tag.getLong("Center"));
        }
        this.radius = tag.getInt("Radius");
        this.poiCacheDirty = true;
    }
}
