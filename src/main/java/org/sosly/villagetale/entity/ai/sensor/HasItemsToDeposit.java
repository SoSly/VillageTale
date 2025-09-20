package org.sosly.villagetale.entity.ai.sensor;

import com.google.common.collect.ImmutableSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.item.ItemStack;
import org.sosly.villagetale.VillageTale;
import org.sosly.villagetale.entity.MemoryModuleTypes;
import org.sosly.villagetale.entity.Villager;
import org.sosly.villagetale.helper.ItemMatcher;

public class HasItemsToDeposit extends Sensor<Villager> {
    private static final int NEARLY_FULL_THRESHOLD = 4;
    private static final int LARGE_QUANTITY_THRESHOLD = 16;
    private static final int FOOD_TO_KEEP = 3;

    public HasItemsToDeposit() {
        super(200);
    }

    @Override
    protected void doTick(ServerLevel level, Villager villager) {
        if (villager.getBrain().hasMemoryValue(MemoryModuleTypes.ITEMS_TO_DEPOSIT.get())) {
            return;
        }

        boolean shouldDeposit = shouldTriggerDeposit(villager);
        if (!shouldDeposit) {
            return;
        }

        Map<ResourceLocation, Integer> itemsToDeposit = calculateItemsToDeposit(villager);
        if (itemsToDeposit.isEmpty()) {
            return;
        }

        villager.getBrain().setMemory(MemoryModuleTypes.ITEMS_TO_DEPOSIT.get(), itemsToDeposit);

        if (VillageTale.LOGGER.isDebugEnabled()) {
            VillageTale.LOGGER.debug("HasItemsToDeposit set ITEMS_TO_DEPOSIT for villager {} with {} items",
                villager.getId(), itemsToDeposit.size());
        }
    }

    private boolean shouldTriggerDeposit(Villager villager) {
        int usedSlots = countUsedSlots(villager);
        boolean nearlyFull = usedSlots >= NEARLY_FULL_THRESHOLD;

        boolean hasLargeQuantityOfNonEssentials = hasLargeQuantityOfNonEssentials(villager);
        
        boolean hasExcessResources = hasExcessResources(villager);

        boolean isEvening = villager.getBrain().isActive(Activity.IDLE);

        return nearlyFull || hasLargeQuantityOfNonEssentials || hasExcessResources || isEvening;
    }

    private int countUsedSlots(Villager villager) {
        int usedSlots = 0;
        for (int i = 0; i < villager.getInventory().getContainerSize(); i++) {
            if (!villager.getInventory().getItem(i).isEmpty()) {
                usedSlots++;
            }
        }
        return usedSlots;
    }

    private boolean hasLargeQuantityOfNonEssentials(Villager villager) {
        Map<ResourceLocation, Integer> itemCounts = new HashMap<>();

        for (int i = 0; i < villager.getInventory().getContainerSize(); i++) {
            ItemStack stack = villager.getInventory().getItem(i);
            if (stack.isEmpty()) {
                continue;
            }

            if (isEssentialItem(stack, villager)) {
                continue;
            }

            ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
            itemCounts.merge(itemId, stack.getCount(), Integer::sum);
        }

        return itemCounts.values().stream().anyMatch(count -> count >= LARGE_QUANTITY_THRESHOLD);
    }
    
    private boolean hasExcessResources(Villager villager) {
        int wantedAmount = ItemMatcher.RESOURCES.getFor(villager).getAmount();
        if (wantedAmount <= 0) {
            return false;
        }
        
        for (int i = 0; i < villager.getInventory().getContainerSize(); i++) {
            ItemStack stack = villager.getInventory().getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            
            if (ItemMatcher.RESOURCES.getFor(villager).getMatcher().test(stack)) {
                if (stack.getCount() > wantedAmount) {
                    return true;
                }
            }
        }
        
        return false;
    }

    private Map<ResourceLocation, Integer> calculateItemsToDeposit(Villager villager) {
        Map<ResourceLocation, Integer> itemsToDeposit = new HashMap<>();
        Map<ResourceLocation, Integer> allItems = new HashMap<>();

        for (int i = 0; i < villager.getInventory().getContainerSize(); i++) {
            ItemStack stack = villager.getInventory().getItem(i);
            if (stack.isEmpty()) {
                continue;
            }

            ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
            allItems.merge(itemId, stack.getCount(), Integer::sum);
        }

        int foodKept = 0;

        for (Map.Entry<ResourceLocation, Integer> entry : allItems.entrySet()) {
            ResourceLocation itemId = entry.getKey();
            int totalCount = entry.getValue();
            ItemStack testStack = new ItemStack(BuiltInRegistries.ITEM.get(itemId));

            // Check if this matches resources (like seeds for farmers)
            if (ItemMatcher.RESOURCES.getFor(villager).getMatcher().test(testStack)) {
                int wantedAmount = ItemMatcher.RESOURCES.getFor(villager).getAmount();
                if (totalCount > wantedAmount) {
                    itemsToDeposit.put(itemId, totalCount - wantedAmount);
                }
                continue;
            }
            
            // Check if this is a tool - keep only what's needed
            if (isEssentialItem(testStack, villager)) {
                continue;
            }

            if (testStack.isEdible() && foodKept < FOOD_TO_KEEP) {
                int toKeep = Math.min(totalCount, FOOD_TO_KEEP - foodKept);
                foodKept += toKeep;
                totalCount -= toKeep;
            }

            if (totalCount > 0) {
                itemsToDeposit.put(itemId, totalCount);
            }
        }

        return itemsToDeposit;
    }

    private boolean isEssentialItem(ItemStack stack, Villager villager) {
        if (ItemMatcher.PROFESSION_TOOL.getFor(villager).getMatcher().test(stack)) {
            return true;
        }

        if (ItemMatcher.RESOURCES.getFor(villager).getMatcher().test(stack)) {
            return true;
        }

        return false;
    }

    @Override
    public Set<MemoryModuleType<?>> requires() {
        return ImmutableSet.of(MemoryModuleTypes.ITEMS_TO_DEPOSIT.get());
    }
}
