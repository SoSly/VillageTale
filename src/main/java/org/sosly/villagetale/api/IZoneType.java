package org.sosly.villagetale.api;

import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

public interface IZoneType {
    ResourceLocation getID();
    boolean isPOI(Level level, BlockPos pos);
    void initialize(Level level, IZoneShape shape);
    CompoundTag serializeNBT();
    void deserializeNBT(CompoundTag nbt);
    void onDatapackReload(JsonObject data);
    boolean supportsItemFilters();
    boolean supportsEntityFilters();
}
