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
import org.sosly.villagetale.zone.Zone;
import org.sosly.villagetale.zone.ZoneRegistry;

public class Rectangle implements IZoneShape {
    public static final ResourceLocation ID = new ResourceLocation(VillageTale.MOD_ID, "rectangle");


    private AABB bounds;

    public Rectangle() {
    }

    public Rectangle(AABB bounds) {
        this.bounds = bounds;
    }

    public AABB getBounds() {
        return bounds;
    }

    @Override
    public boolean containsPosition(BlockPos pos) {
        return bounds.contains(pos.getX(), pos.getY(), pos.getZ());
    }
    
    @Override
    public boolean containsPosition(BlockPos pos, int buffer) {
        return bounds.inflate(buffer, 0, buffer).contains(pos.getX(), pos.getY(), pos.getZ());
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

        return BlockPos.betweenClosedStream(bounds)
                .filter(pos -> isPOI.test(pos))
                .map(BlockPos::immutable)
                .toList();
    }

    @Override
    public BlockPos getStartPosition() {
        return BlockPos.containing(bounds.getCenter());
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        BlockPos min = new BlockPos((int)bounds.minX, (int)bounds.minY, (int)bounds.minZ);
        BlockPos max = new BlockPos((int)bounds.maxX, (int)bounds.maxY, (int)bounds.maxZ);
        tag.putLong("min", min.asLong());
        tag.putLong("max", max.asLong());
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        BlockPos min = BlockPos.of(tag.getLong("min"));
        BlockPos max = BlockPos.of(tag.getLong("max"));
        bounds = new AABB(min, max);
    }

    public static Builder builder(Level level, IVillageCapability village, int ordinal) {
        return new Builder(level, village, ordinal);
    }

    public static class Builder {
        private final Level level;
        private final IVillageCapability village;
        private final int ordinal;
        private AABB bounds;

        private IZoneType type;

        private Builder(Level level, IVillageCapability village, int ordinal) {
            this.level = level;
            this.village = village;
            this.ordinal = ordinal;
        }

        public Builder setBounds(AABB bounds) {
            this.bounds = bounds;
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

            if (bounds == null) {
                throw new IllegalStateException("Rectangle Zone has no bounds");
            }

            return new Zone(level, UUID.randomUUID(), village, ordinal, new Rectangle(bounds), type);
        }
    }
}
