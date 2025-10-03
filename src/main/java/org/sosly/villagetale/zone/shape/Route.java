package org.sosly.villagetale.zone.shape;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.sosly.villagetale.VillageTale;
import org.sosly.villagetale.api.IZoneShape;
import org.sosly.villagetale.api.IZoneType;
import org.sosly.villagetale.api.capability.IVillageCapability;
import org.sosly.villagetale.network.packets.clientbound.ZoneBoundaryPacket;
import org.sosly.villagetale.zone.Zone;
import org.sosly.villagetale.zone.ZoneRegistry;

public class Route implements IZoneShape {
    public static final ResourceLocation ID = new ResourceLocation(VillageTale.MOD_ID, "route");
    private List<BlockPos> points = new ArrayList<>();

    public Route() {}

    public List<BlockPos> getPath() {
        return points;
    }

    public void addPoint(BlockPos pos) {
        points.add(pos);
    }

    public void clearPath() {
        points.clear();
    }

    @Override
    public boolean containsPosition(BlockPos pos) {
        return points.contains(pos);
    }

    @Override
    public ResourceLocation getID() {
        return ID;
    }

    @Override
    public List<BlockPos> getPOIs(Level level, Predicate<BlockPos> isPOI) {
        if (level == null) {
            return Collections.emptyList();
        }
        return points.stream()
                .filter(pos -> isPOI.test(pos))
                .toList();
    }

    @Override
    public BlockPos getStartPosition() {
        return points.isEmpty() ? BlockPos.ZERO : points.get(0);
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();

        ListTag points = new ListTag();
        for (BlockPos pos : this.points) {
            points.add(LongTag.valueOf(pos.asLong()));
        }
        tag.put("points", points);
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        this.points.clear();
        ListTag points = tag.getList("points", 10);
        for (int i = 0; i < points.size(); ++i) {
            long point = points.getInt(i);
            this.points.add(BlockPos.of(point));
        }
    }

    @Override
    public ZoneBoundaryPacket createBoundaryPacket(UUID zoneId, UUID villageId) {
        if (points.isEmpty()) {
            return null;
        }

        BlockPos first = points.get(0);
        AABB bounds = new AABB(first);
        for (BlockPos waypoint : points) {
            bounds = bounds.minmax(new AABB(waypoint));
        }

        return new ZoneBoundaryPacket(zoneId, villageId, getID(), bounds, null, 0, 0, points);
    }

    public static Builder builder(Level level, IVillageCapability village, int ordinal) {
        return new Builder(level, village, ordinal);
    }

    public static class Builder {
        private final Level level;
        private final IVillageCapability village;
        private final int ordinal;

        private IZoneType type;

        private Builder(Level level, IVillageCapability village, int ordinal) {
            this.level = level;
            this.village = village;
            this.ordinal = ordinal;
        }

        public Builder setType(ResourceLocation type) {
            this.type = ZoneRegistry.INSTANCE.type(type);
            return this;
        }

        public Zone build() {
            if (type == null) {
                throw new IllegalStateException("Zone has no type");
            }

            return new Zone(level, UUID.randomUUID(), village, ordinal, new Route(), type);
        }
    }
}
