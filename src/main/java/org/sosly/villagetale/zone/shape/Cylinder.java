package org.sosly.villagetale.zone.shape;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.sosly.villagetale.VillageTale;
import org.sosly.villagetale.api.capability.IVillageCapability;
import org.sosly.villagetale.api.IZoneShape;
import org.sosly.villagetale.api.IZoneType;
import org.sosly.villagetale.network.packets.clientbound.ZoneBoundaryPacket;
import org.sosly.villagetale.zone.Zone;
import org.sosly.villagetale.zone.ZoneRegistry;

public class Cylinder implements IZoneShape {
    public static final ResourceLocation ID = new ResourceLocation(VillageTale.MOD_ID, "cylinder");

    private BlockPos baseCenter;
    private int radius;
    private int height;

    public Cylinder() {}

    public Cylinder(BlockPos baseCenter, int radius, int height) {
        this.baseCenter = baseCenter;
        this.radius = radius;
        this.height = height;
    }

    public BlockPos getBaseCenter() {
        return baseCenter;
    }

    public int getRadius() {
        return radius;
    }

    public int getHeight() {
        return height;
    }

    @Override
    public boolean containsPosition(BlockPos pos) {
        return containsPosition(pos, 0);
    }

    @Override
    public boolean containsPosition(BlockPos pos, int buffer) {
        if (baseCenter == null || pos == null) {
            return false;
        }

        if (pos.getY() < baseCenter.getY() - buffer || pos.getY() >= baseCenter.getY() + height + buffer) {
            return false;
        }

        double dx = pos.getX() - baseCenter.getX();
        double dz = pos.getZ() - baseCenter.getZ();
        double distanceSquared = dx * dx + dz * dz;
        int expandedRadius = radius + buffer;
        return distanceSquared <= expandedRadius * expandedRadius;
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

        double radiusSq = radius * radius;

        return BlockPos.betweenClosedStream(
                baseCenter.offset(-radius, 0, -radius),
                baseCenter.offset(radius, height - 1, radius)
            )
            .filter(pos -> {
                double dx = pos.getX() - baseCenter.getX();
                double dz = pos.getZ() - baseCenter.getZ();
                return dx * dx + dz * dz <= radiusSq;
            })
            .filter(pos -> isPOI.test(pos))
            .map(BlockPos::immutable)
            .toList();
    }

    @Override
    public BlockPos getStartPosition() {
        return this.baseCenter;
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("radius", this.radius);
        tag.putInt("height", this.height);
        tag.putLong("baseCenter", this.baseCenter.asLong());
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        this.radius = nbt.getInt("radius");
        this.height = nbt.getInt("height");
        this.baseCenter = BlockPos.of(nbt.getLong("baseCenter"));
    }

    @Override
    public ZoneBoundaryPacket createBoundaryPacket(UUID zoneId, UUID villageId) {
        AABB bounds = new AABB(
            baseCenter.getX() - radius, baseCenter.getY(), baseCenter.getZ() - radius,
            baseCenter.getX() + radius, baseCenter.getY() + height, baseCenter.getZ() + radius
        );
        return new ZoneBoundaryPacket(zoneId, villageId, getID(), bounds, baseCenter, radius, height, null);
    }

    public static Builder builder(Level level, IVillageCapability village, int ordinal) {
        return new Builder(level, village, ordinal);
    }

    public static class Builder {
        private final Level level;
        private final IVillageCapability village;
        private final int ordinal;
        private BlockPos baseCenter;
        private int radius;
        private int height = 4;

        private IZoneType type;

        private Builder(Level level, IVillageCapability village, int ordinal) {
            this.level = level;
            this.village = village;
            this.ordinal = ordinal;
        }

        public Builder setBaseCenter(BlockPos pos) {
            this.baseCenter = pos;
            return this;
        }

        public Builder setRadius(int radius) {
            this.radius = radius;
            return this;
        }

        public Builder setHeight(int height) {
            this.height = height;
            return this;
        }

        public Builder setType(ResourceLocation type) {
            this.type = ZoneRegistry.INSTANCE.type(type);
            return this;
        }

        public Zone build() {
            if (type == null) {
                throw new IllegalStateException("Zone has no type");
            }

            if (baseCenter == null) {
                throw new IllegalStateException("Cylinder Zone has no base center");
            }

            if (radius <= 0) {
                throw new IllegalStateException("Cylinder Zone has no radius");
            }

            if (height <= 0) {
                throw new IllegalStateException("Cylinder Zone has no height");
            }

            return new Zone(level, UUID.randomUUID(), village, ordinal, new Cylinder(baseCenter, radius, height), type);
        }
    }
}
