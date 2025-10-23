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
import org.sosly.villagetale.network.packets.clientbound.ZoneBoundary;
import org.sosly.villagetale.zone.Zone;
import org.sosly.villagetale.zone.ZoneRegistry;

public class Point implements IZoneShape {
    public static final ResourceLocation ID = new ResourceLocation(VillageTale.MOD_ID, "point");

    private BlockPos pos;

    public Point() {
    }

    public Point(BlockPos pos) {
        this.pos = pos;
    }

    public BlockPos getPos() {
        return pos;
    }

    @Override
    public boolean containsPosition(BlockPos pos) {
        return this.pos.equals(pos);
    }

    @Override
    public ResourceLocation getID() {
        return ID;
    }

    @Override
    public List<BlockPos> getPOIs(Predicate<BlockPos> isPOI) {
        if (!isPOI.test(pos)) {
            return Collections.emptyList();
        }

        return Collections.singletonList(this.pos);
    }

    @Override
    public BlockPos getStartPosition() {
        return pos;
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putLong("pos", pos.asLong());
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        pos = BlockPos.of(tag.getLong("pos"));
    }

    @Override
    public ZoneBoundary createBoundaryPacket(UUID zoneId, UUID villageId) {
        AABB bounds = new AABB(pos);
        return new ZoneBoundary(zoneId, villageId, getID(), bounds);
    }

    public static Builder builder(Level level, IVillageCapability village, int ordinal) {
        return new Builder(level, village, ordinal);
    }

    public static class Builder {
        private final Level level;
        private final IVillageCapability village;
        private final int ordinal;
        private BlockPos pos;

        private IZoneType type;

        private Builder(Level level, IVillageCapability village, int ordinal) {
            this.level = level;
            this.village = village;
            this.ordinal = ordinal;
        }

        public Builder setPos(BlockPos pos) {
            this.pos = pos;
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

            if (pos == null) {
                throw new IllegalStateException("Point Zone has no pos");
            }

            return new Zone(level, UUID.randomUUID(), village, ordinal, new Point(pos), type);
        }
    }
}
