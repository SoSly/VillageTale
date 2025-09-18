package org.sosly.villagetale.helper;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import org.sosly.villagetale.entity.FakePlayer;

import java.util.function.Predicate;

public class ContainerHelper {

    public static void openContainer(ServerLevel level, BlockPos containerPos) {
        BlockEntity blockEntity = level.getBlockEntity(containerPos);
        if (!(blockEntity instanceof Container)) {
            return;
        }

        level.blockEvent(containerPos, level.getBlockState(containerPos).getBlock(), 1, 1);

        if (blockEntity instanceof RandomizableContainerBlockEntity container) {
            FakePlayer fakePlayer = new FakePlayer(level);
            container.startOpen(fakePlayer);
        }
    }

    public static void closeContainer(ServerLevel level, BlockPos containerPos) {
        // Don't call this method - containers auto-close when FakePlayer isn't in world
        // ContainerOpenersCounter.recheckOpeners() finds no real players and closes automatically
        BlockEntity blockEntity = level.getBlockEntity(containerPos);
        if (!(blockEntity instanceof Container)) {
            return;
        }

        level.blockEvent(containerPos, level.getBlockState(containerPos).getBlock(), 1, 0);

        if (blockEntity instanceof RandomizableContainerBlockEntity container) {
            FakePlayer fakePlayer = new FakePlayer(level);
            container.stopOpen(fakePlayer);
        }
    }

    public static ItemStack extractItemFromContainer(ServerLevel level, BlockPos containerPos, Predicate<ItemStack> itemMatcher) {
        BlockEntity blockEntity = level.getBlockEntity(containerPos);
        if (!(blockEntity instanceof Container container)) {
            return ItemStack.EMPTY;
        }

        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            if (itemMatcher.test(stack)) {
                ItemStack extracted = stack.copy();
                extracted.setCount(1);
                stack.shrink(1);
                container.setChanged();
                return extracted;
            }
        }
        return ItemStack.EMPTY;
    }

    public static ItemStack extractItemFromContainer(ServerLevel level, BlockPos containerPos, ItemStack targetItem) {
        BlockEntity blockEntity = level.getBlockEntity(containerPos);
        if (!(blockEntity instanceof Container container)) {
            return ItemStack.EMPTY;
        }

        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            if (ItemStack.isSameItem(stack, targetItem)) {
                ItemStack extracted = stack.copy();
                extracted.setCount(Math.min(extracted.getCount(), targetItem.getCount()));
                stack.shrink(extracted.getCount());
                container.setChanged();
                return extracted;
            }
        }
        return ItemStack.EMPTY;
    }

    public static boolean hasMatchingItem(ServerLevel level, BlockPos containerPos, Predicate<ItemStack> itemMatcher) {
        BlockEntity blockEntity = level.getBlockEntity(containerPos);
        if (!(blockEntity instanceof Container container)) {
            return false;
        }

        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            if (itemMatcher.test(stack)) {
                return true;
            }
        }
        return false;
    }

    public static ResourceLocation getFirstMatchingItemId(ServerLevel level, BlockPos containerPos, Predicate<ItemStack> itemMatcher) {
        BlockEntity blockEntity = level.getBlockEntity(containerPos);
        if (!(blockEntity instanceof Container container)) {
            return null;
        }

        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            if (itemMatcher.test(stack)) {
                return BuiltInRegistries.ITEM.getKey(stack.getItem());
            }
        }
        return null;
    }

}
