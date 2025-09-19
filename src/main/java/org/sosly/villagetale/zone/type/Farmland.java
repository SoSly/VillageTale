package org.sosly.villagetale.zone.type;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ToolActions;
import org.sosly.villagetale.VillageTale;
import org.sosly.villagetale.api.IZoneType;

public class Farmland implements IZoneType {
    public static final ResourceLocation ID = new ResourceLocation(VillageTale.MOD_ID, "farmland");

    @Override
    public ResourceLocation getID() {
        return ID;
    }

    @Override
    public boolean isPOI(Level level, BlockPos pos) {
        return (isHarvestableBlock(level, pos) || isPlantableBlock(level, pos) || isTillableBlock(level, pos));
    }

    @Override
    public CompoundTag serializeNBT() {
        return null;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
    }

    public static boolean isHarvestableBlock(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof CropBlock)) {
            return false;
        }

        CropBlock block = (CropBlock) state.getBlock();
        return block.getAge(state) >= block.getMaxAge();
    }

    public static boolean isPlantableBlock(Level level, BlockPos pos) {
        CropBlock crop = (CropBlock) Blocks.WHEAT;
        BlockState state = crop.defaultBlockState();

        return crop.canSurvive(state, level, pos);
    }

    public static boolean isTillableBlock(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        BlockState above = level.getBlockState(pos.above());
        ItemStack hoe = new ItemStack(Items.WOODEN_HOE);

        if (!above.isAir()) {
            return false;
        }

        BlockState tilled = state.getToolModifiedState(
                new UseOnContext(level, null, InteractionHand.MAIN_HAND, hoe,
                        new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false)),
                ToolActions.HOE_TILL,false);

        return tilled != null;
    }
}
