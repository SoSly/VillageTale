package org.sosly.villagetale.helper;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import org.sosly.villagetale.entity.FakePlayer;

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

    public static ItemStack extractItemFromContainer(ServerLevel level, BlockPos containerPos, Predicate<ItemStack> itemMatcher, int maxAmount) {
        BlockEntity blockEntity = level.getBlockEntity(containerPos);
        if (!(blockEntity instanceof Container container)) {
            return ItemStack.EMPTY;
        }

        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            if (itemMatcher.test(stack)) {
                ItemStack extracted = stack.copy();
                int toExtract = Math.min(stack.getCount(), maxAmount);
                extracted.setCount(toExtract);
                stack.shrink(toExtract);
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

    public static int depositItemToContainer(ServerLevel level, BlockPos containerPos, ItemStack itemToDeposit, int maxAmount) {
        BlockEntity blockEntity = level.getBlockEntity(containerPos);
        if (!(blockEntity instanceof Container container)) {
            return 0;
        }

        int remainingToDeposit = Math.min(itemToDeposit.getCount(), maxAmount);
        int deposited = 0;

        for (int i = 0; i < container.getContainerSize() && remainingToDeposit > 0; i++) {
            ItemStack existingStack = container.getItem(i);

            if (existingStack.isEmpty()) {
                int maxStackSize = container.getMaxStackSize();
                int depositAmount = Math.min(remainingToDeposit, Math.min(itemToDeposit.getMaxStackSize(), maxStackSize));
                ItemStack newStack = itemToDeposit.copy();
                newStack.setCount(depositAmount);
                container.setItem(i, newStack);
                deposited += depositAmount;
                remainingToDeposit -= depositAmount;
                continue;
            }

            if (!ItemStack.isSameItemSameTags(existingStack, itemToDeposit)) {
                continue;
            }

            int spaceAvailable = existingStack.getMaxStackSize() - existingStack.getCount();
            int depositAmount = Math.min(remainingToDeposit, spaceAvailable);
            if (depositAmount <= 0) {
                continue;
            }

            existingStack.grow(depositAmount);
            deposited += depositAmount;
            remainingToDeposit -= depositAmount;
        }

        if (deposited > 0) {
            container.setChanged();
        }

        return deposited;
    }

    public static boolean hasAvailableSpace(ServerLevel level, BlockPos containerPos, ItemStack itemToCheck) {
        BlockEntity blockEntity = level.getBlockEntity(containerPos);
        if (!(blockEntity instanceof Container container)) {
            return false;
        }

        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack existingStack = container.getItem(i);

            if (existingStack.isEmpty()) {
                return true;
            } else if (ItemStack.isSameItemSameTags(existingStack, itemToCheck)) {
                int spaceAvailable = existingStack.getMaxStackSize() - existingStack.getCount();
                if (spaceAvailable > 0) {
                    return true;
                }
            }
        }

        return false;
    }

    public static boolean hasAvailableSpace(ServerLevel level, BlockPos containerPos, net.minecraft.world.item.Item item) {
        BlockEntity blockEntity = level.getBlockEntity(containerPos);
        if (!(blockEntity instanceof Container container)) {
            return false;
        }

        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack existingStack = container.getItem(i);

            if (existingStack.isEmpty()) {
                return true;
            } else if (existingStack.is(item)) {
                int spaceAvailable = existingStack.getMaxStackSize() - existingStack.getCount();
                if (spaceAvailable > 0) {
                    return true;
                }
            }
        }

        return false;
    }

    public static Map<ResourceLocation, Integer> tryDepositFromInventory(
            ServerLevel level,
            BlockPos containerPos,
            SimpleContainer inventory,
            Map<ResourceLocation, Integer> itemsToDeposit) {
        
        if (itemsToDeposit == null || itemsToDeposit.isEmpty()) {
            return new HashMap<>();
        }

        Map<ResourceLocation, Integer> remaining = new HashMap<>(itemsToDeposit);

        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }

            ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
            Integer wantedAmount = itemsToDeposit.get(itemId);
            if (wantedAmount == null || wantedAmount <= 0) {
                continue;
            }

            int deposited = depositItemToContainer(level, containerPos, stack, wantedAmount);
            if (deposited <= 0) {
                continue;
            }

            stack.shrink(deposited);
            int leftover = wantedAmount - deposited;

            if (leftover <= 0) {
                remaining.remove(itemId);
            } else {
                remaining.put(itemId, leftover);
            }
        }

        return remaining;
    }

}
