package org.sosly.villagetale.entity.ai.sensor;

import com.google.common.collect.ImmutableSet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.item.ItemStack;
import org.sosly.villagetale.VillageTale;
import org.sosly.villagetale.api.IWantedItem;
import org.sosly.villagetale.entity.MemoryModuleTypes;
import org.sosly.villagetale.entity.Villager;
import org.sosly.villagetale.helper.ItemMatcher;

import java.util.List;
import java.util.Set;

public class HasFood extends Sensor<Villager> {

    public HasFood() {
        super(200);
    }

    @Override
    protected void doTick(ServerLevel level, Villager villager) {
        boolean hasFood = hasFood(villager);
        boolean isHungry = villager.getBrain().getMemory(MemoryModuleTypes.IS_HUNGRY.get()).orElse(false);
        boolean isStarving = villager.getBrain().getMemory(MemoryModuleTypes.IS_STARVING.get()).orElse(false);

        boolean needsFood = !hasFood && (isHungry || isStarving);
        boolean hasExistingWant = villager.getBrain().hasMemoryValue(MemoryModuleTypes.WANTED_ITEM.get());

        if (needsFood && (!hasExistingWant || isStarving)) {
            List<IWantedItem> foodWants = ItemMatcher.FOOD.getFor(villager);
            if (!foodWants.isEmpty()) {
                villager.getBrain().setMemoryWithExpiry(MemoryModuleTypes.WANTED_ITEM.get(), foodWants.get(0), 2400L);
            }
            villager.getBrain().eraseMemory(MemoryModuleTypes.ALREADY_SCANNED_STORAGES.get());
            villager.getBrain().eraseMemory(MemoryModuleTypes.FOUND_ITEM.get());

            VillageTale.LOGGER.debug("HasFood set WANTED_ITEM to FOOD for villager {}", villager.getId());
            return;
        }

        if (!needsFood && !hasExistingWant) {
            return;
        }

        IWantedItem currentWant = villager.getBrain().getMemory(MemoryModuleTypes.WANTED_ITEM.get()).orElse(null);
        if (currentWant == null) {
            return;
        }
        
        List<IWantedItem> foodWants = ItemMatcher.FOOD.getFor(villager);
        if (foodWants.isEmpty() || !currentWant.equals(foodWants.get(0))) {
            return;
        }

        villager.getBrain().eraseMemory(MemoryModuleTypes.WANTED_ITEM.get());

        VillageTale.LOGGER.debug("HasFood cleared WANTED_ITEM for villager {}", villager.getId());
    }

    private boolean hasFood(Villager villager) {
        for (int i = 0; i < villager.getInventory().getContainerSize(); i++) {
            ItemStack stack = villager.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.isEdible()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Set<MemoryModuleType<?>> requires() {
        return ImmutableSet.of(
            MemoryModuleTypes.IS_HUNGRY.get(),
            MemoryModuleTypes.IS_STARVING.get(),
            MemoryModuleTypes.WANTED_ITEM.get(),
            MemoryModuleTypes.ALREADY_SCANNED_STORAGES.get(),
            MemoryModuleTypes.FOUND_ITEM.get()
        );
    }
}
