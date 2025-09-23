package org.sosly.villagetale.zone.type;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.CraftingTableBlock;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.sosly.villagetale.VillageTale;
import org.sosly.villagetale.api.IZoneType;

public class Kitchen implements IZoneType {
    public static final ResourceLocation ID = new ResourceLocation(VillageTale.MOD_ID, "kitchen");


    @Override
    public ResourceLocation getID() {
        return ID;
    }

    @Override
    public boolean isPOI(Level level, BlockPos pos) {
        return isCraftingBlock(level, pos) || isCookingBlock(level, pos);
    }

    @Override
    public CompoundTag serializeNBT() {
        return null;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
    }

    public static boolean isCraftingBlock(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);

        if (state.getBlock() instanceof CraftingTableBlock) {
            return true;
        }

        String blockName = state.getBlock().getDescriptionId().toLowerCase();
        return blockName.contains("cutting");
    }

    public static boolean isCookingBlock(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);

        if (state.getBlock() instanceof AbstractFurnaceBlock) {
            return true;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof AbstractFurnaceBlockEntity) {
            return true;
        }

        String blockName = state.getBlock().getDescriptionId().toLowerCase();
        return blockName.contains("furnace")
            || blockName.contains("oven")
            || blockName.contains("stove")
            || blockName.contains("smoker");
    }
}
