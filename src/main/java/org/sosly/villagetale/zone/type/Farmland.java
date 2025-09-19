package org.sosly.villagetale.zone.type;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.sosly.villagetale.VillageTale;
import org.sosly.villagetale.api.IZoneType;
import org.sosly.villagetale.data.TagKeys;

public class Farmland implements IZoneType {
    public static final ResourceLocation ID = new ResourceLocation(VillageTale.MOD_ID, "farmland");

    @Override
    public ResourceLocation getID() {
        return ID;
    }

    @Override
    public boolean isPOI(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        BlockState above = level.getBlockState(pos.above());

        if (!above.isAir()) {
            return false;
        }

        if (!state.is(TagKeys.FARMABLE) && !state.is(BlockTags.MAINTAINS_FARMLAND)) {
            return false;
        }

        return true;
    }

    @Override
    public CompoundTag serializeNBT() {
        return null;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
    }
}
