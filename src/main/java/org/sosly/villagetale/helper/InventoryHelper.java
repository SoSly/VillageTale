package org.sosly.villagetale.helper;

import net.minecraft.tags.ItemTags;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import org.sosly.villagetale.api.IVillageZone;
import org.sosly.villagetale.entity.Villager;

import java.util.List;

public class InventoryHelper {

    public static boolean tryAddToInventory(Container inventory, ItemStack stack) {
        if (stack.isEmpty()) {
            return true;
        }

        ItemStack toAdd = stack.copy();

        if (!tryMergeWithExisting(inventory, toAdd)) {
            tryAddToEmptySlot(inventory, toAdd);
        }

        boolean success = toAdd.isEmpty();
        if (success) {
            stack.setCount(0);
        }

        return success;
    }

    public static boolean canAddToInventory(Container inventory, ItemStack stack) {
        if (stack.isEmpty()) {
            return true;
        }

        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack slotStack = inventory.getItem(i);

            if (slotStack.isEmpty()) {
                return true;
            }

            if (!ItemStack.isSameItemSameTags(slotStack, stack)) {
                continue;
            }

            int spaceLeft = slotStack.getMaxStackSize() - slotStack.getCount();
            if (spaceLeft > 0) {
                return true;
            }
        }

        return false;
    }

    public static ItemStack getTool(Villager villager) {
        Container inventory = villager.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            if (inventory.getItem(i).is(ItemTags.HOES)) {
                return inventory.getItem(i);
            }
        }
        return ItemStack.EMPTY;
    }

    public static ItemStack getSeeds(Villager villager) {
        Container inventory = villager.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.is(ItemTags.VILLAGER_PLANTABLE_SEEDS)) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    public static ItemStack getSeeds(Villager villager, IVillageZone zone) {
        if (zone == null) {
            return getSeeds(villager);
        }

        List<ItemStack> wantedItems = zone.getFilter();
        if (wantedItems.isEmpty()) {
            return getSeeds(villager);
        }

        Container inventory = villager.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (!stack.is(ItemTags.VILLAGER_PLANTABLE_SEEDS)) {
                continue;
            }

            for (ItemStack wanted : wantedItems) {
                if (ItemStack.isSameItem(wanted, stack)) {
                    return stack;
                }
            }
        }

        return ItemStack.EMPTY;
    }

    private static boolean tryMergeWithExisting(Container inventory, ItemStack stack) {
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack slotStack = inventory.getItem(i);

            if (slotStack.isEmpty()) {
                continue;
            }

            if (!ItemStack.isSameItemSameTags(slotStack, stack)) {
                continue;
            }

            int spaceLeft = slotStack.getMaxStackSize() - slotStack.getCount();
            if (spaceLeft <= 0) {
                continue;
            }

            int toTransfer = Math.min(spaceLeft, stack.getCount());
            slotStack.grow(toTransfer);
            stack.shrink(toTransfer);

            if (stack.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private static void tryAddToEmptySlot(Container inventory, ItemStack stack) {
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            if (!inventory.getItem(i).isEmpty()) {
                continue;
            }

            inventory.setItem(i, stack.copy());
            stack.setCount(0);
            return;
        }
    }
}
