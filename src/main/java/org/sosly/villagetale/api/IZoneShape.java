package org.sosly.villagetale.api;

import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.sosly.villagetale.network.packets.clientbound.ZoneBoundary;

public interface IZoneShape {
    boolean containsPosition(BlockPos pos);

    default boolean containsPosition(BlockPos pos, int buffer) {
        return containsPosition(pos);
    }

    ResourceLocation getID();

    List<BlockPos> getPOIs(Level level, Predicate<BlockPos> isPOI);

    BlockPos getStartPosition();

    CompoundTag serializeNBT();

    void deserializeNBT(CompoundTag nbt);

    ZoneBoundary createBoundaryPacket(UUID zoneId, UUID villageId);
}
