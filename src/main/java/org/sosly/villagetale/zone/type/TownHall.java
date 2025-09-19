package org.sosly.villagetale.zone.type;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.sosly.villagetale.VillageTale;
import org.sosly.villagetale.api.IZoneType;
import org.sosly.villagetale.block.BlockTypes;

public class TownHall implements IZoneType {
    public static final ResourceLocation ID = new ResourceLocation(VillageTale.MOD_ID, "townhall");

    @Override
    public ResourceLocation getID() {
        return ID;
    }

    @Override
    public boolean isPoI(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.is(BlockTypes.TOWNHALL.get());
    }

    @Override
    public CompoundTag serializeNBT() {
        return null;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
    }
}
