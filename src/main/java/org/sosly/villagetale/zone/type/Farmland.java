package org.sosly.villagetale.zone.type;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ToolActions;
import org.sosly.villagetale.VillageTale;

public class Farmland extends AbstractZoneType {
    public static final ResourceLocation ID = new ResourceLocation(VillageTale.MOD_ID, "farmland");

    @Override
    public ResourceLocation getID() {
        return ID;
    }

    public static boolean isHarvestableBlock(Level level, BlockPos pos) {
        BlockPos above = pos.above();
        BlockState state = level.getBlockState(above);
        if (!(state.getBlock() instanceof CropBlock)) {
            return false;
        }

        CropBlock block = (CropBlock) state.getBlock();
        return block.getAge(state) >= block.getMaxAge();
    }

    public static boolean isPlantableBlock(Level level, BlockPos pos, ItemStack seed) {
        BlockPos above = pos.above();
        if (!level.getBlockState(above).isAir()) {
            return false;
        }

        if (seed.getItem() instanceof BlockItem blockItem) {
            Block block = blockItem.getBlock();
            BlockState defaultState = block.defaultBlockState();
            return block.canSurvive(defaultState, level, above);
        }

        CropBlock crop = (CropBlock) Blocks.WHEAT;
        BlockState cropState = crop.defaultBlockState();
        return crop.canSurvive(cropState, level, above);
    }

    public static boolean isTillableBlock(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        BlockState above = level.getBlockState(pos.above());

        if (!above.isAir()) {
            return false;
        }

        if (state.is(Blocks.ROOTED_DIRT)) {
            return true;
        }

        ItemStack hoe = new ItemStack(Items.WOODEN_HOE);
        BlockState tilled = state.getToolModifiedState(
            new UseOnContext(level, null, InteractionHand.MAIN_HAND, hoe,
                new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false)),
            ToolActions.HOE_TILL, true);

        return tilled != null;
    }
}
