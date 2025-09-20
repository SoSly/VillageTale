package org.sosly.villagetale.helper;

import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

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