package org.sosly.villagetale.zone.shape;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.sosly.villagetale.VillageTale;
import org.sosly.villagetale.api.capability.IVillageCapability;
import org.sosly.villagetale.api.IZoneShape;
import org.sosly.villagetale.api.IZoneType;
import org.sosly.villagetale.zone.Zone;
import org.sosly.villagetale.zone.ZoneRegistry;

public class Sphere implements IZoneShape {
    public static final ResourceLocation ID = new ResourceLocation(VillageTale.MOD_ID, "sphere");

    private BlockPos center;
    private int radius;

    public Sphere() {}

    public Sphere(BlockPos center, int radius) {
        this.center = center;
        this.radius = radius;
    }

    public BlockPos getCenter() {
        return center;
    }

    public int getRadius() {
        return radius;
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
    public ResourceLocation getID() {
        return ID;
    }

    @Override
    public List<BlockPos> getPOIs(Level level, Predicate<BlockPos> isPOI) {
        if (level == null) {
            return Collections.emptyList();
        }

        double radiusSq = radius * radius;  // avoid sqrt in the loop

        return BlockPos.betweenClosedStream(
                center.offset(-radius, -radius, -radius),
                center.offset(radius, radius, radius)
            )
            .filter(pos -> center.distSqr(pos) <= radiusSq)
            .filter(pos -> isPOI.test(pos))
            .map(BlockPos::immutable)
            .toList();
    }

    @Override
    public BlockPos getStartPosition() {
        return this.center;
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("radius", this.radius);
        tag.putLong("center", this.center.asLong());
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        this.radius = nbt.getInt("radius");
        this.center = BlockPos.of(nbt.getLong("center"));
    }

    public static Builder builder(Level level, IVillageCapability village, int ordinal) {
        return new Builder(level, village, ordinal);
    }

    public static class Builder {
        private final Level level;
        private final IVillageCapability village;
        private final int ordinal;
        private BlockPos center;
        private int radius;

        private IZoneType type;

        private Builder(Level level, IVillageCapability village, int ordinal) {
            this.level = level;
            this.village = village;
            this.ordinal = ordinal;
        }

        public Builder setCenter(BlockPos pos) {
            this.center = pos;
            return this;
        }

        public Builder setRadius(int radius) {
            this.radius = radius;
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

            if (center == null) {
                throw new IllegalStateException("Sphere Zone has no center");
            }

            if (radius <= 0) {
                throw new IllegalStateException("Sphere Zone has no radius");
            }

            return new Zone(level, UUID.randomUUID(), village, ordinal, new Sphere(center, radius), type);
        }
    }
}
