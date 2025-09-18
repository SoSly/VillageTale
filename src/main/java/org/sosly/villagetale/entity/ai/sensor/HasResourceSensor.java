package org.sosly.villagetale.entity.ai.sensor;

import com.google.common.collect.ImmutableSet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.item.ItemStack;
import org.sosly.villagetale.VillageTale;
import org.sosly.villagetale.api.data.IWantedItem;
import org.sosly.villagetale.entity.MemoryModuleTypes;
import org.sosly.villagetale.entity.Villager;
import org.sosly.villagetale.data.WantedItem;
import org.sosly.villagetale.helper.ItemMatcher;

import java.util.Set;

public class HasResourceSensor extends Sensor<Villager> {

    private static final int REQUIRED_RESOURCE_COUNT = 16;

    public HasResourceSensor() {
        super(200);
    }

    @Override
    protected void doTick(ServerLevel level, Villager villager) {
        IWantedItem requiredResources = villager.getProfession().getAlwaysWantedItems().orElse(null);
        
        if (requiredResources == null || requiredResources == IWantedItem.EMPTY) {
            return;
        }

        int resourceCount = countResources(villager, requiredResources);
        boolean hasEnoughResources = resourceCount >= REQUIRED_RESOURCE_COUNT;
        boolean hasExistingWant = villager.getBrain().hasMemoryValue(MemoryModuleTypes.WANTED_ITEM.get());

        if (!hasEnoughResources && !hasExistingWant) {
            villager.getBrain().setMemory(MemoryModuleTypes.WANTED_ITEM.get(), ItemMatcher.RESOURCES.getFor(villager));
            villager.getBrain().eraseMemory(MemoryModuleTypes.ALREADY_SCANNED_STORAGES.get());
            villager.getBrain().eraseMemory(MemoryModuleTypes.FOUND_ITEM.get());
            
            if (VillageTale.LOGGER.isDebugEnabled()) {
                VillageTale.LOGGER.debug("HasResourceSensor set WANTED_ITEM to RESOURCES for villager {} (has {}/{})", 
                    villager.getId(), resourceCount, REQUIRED_RESOURCE_COUNT);
            }
            return;
        }

        if (!hasEnoughResources || !hasExistingWant) {
            return;
        }

        var currentWant = villager.getBrain().getMemory(MemoryModuleTypes.WANTED_ITEM.get()).orElse(null);
        if (currentWant == null || !currentWant.equals(ItemMatcher.RESOURCES.getFor(villager))) {
            return;
        }

        villager.getBrain().eraseMemory(MemoryModuleTypes.WANTED_ITEM.get());
        
        if (VillageTale.LOGGER.isDebugEnabled()) {
            VillageTale.LOGGER.debug("HasResourceSensor cleared WANTED_ITEM for villager {} (has {}/{})", 
                villager.getId(), resourceCount, REQUIRED_RESOURCE_COUNT);
        }
    }

    private int countResources(Villager villager, IWantedItem requiredResources) {
        int count = 0;
        for (int i = 0; i < villager.getInventory().getContainerSize(); i++) {
            ItemStack stack = villager.getInventory().getItem(i);
            if (!stack.isEmpty() && requiredResources.getMatcher().test(stack)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    @Override
    public Set<MemoryModuleType<?>> requires() {
        return ImmutableSet.of(
            MemoryModuleTypes.WANTED_ITEM.get(),
            MemoryModuleTypes.ALREADY_SCANNED_STORAGES.get(),
            MemoryModuleTypes.FOUND_ITEM.get()
        );
    }
}