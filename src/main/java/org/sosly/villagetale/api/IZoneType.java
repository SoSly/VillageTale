package org.sosly.villagetale.api;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

public interface IZoneType {
    ResourceLocation getID();
    boolean isPoI(Level level, BlockPos pos);
    CompoundTag serializeNBT();
    void deserializeNBT(CompoundTag nbt);
}
