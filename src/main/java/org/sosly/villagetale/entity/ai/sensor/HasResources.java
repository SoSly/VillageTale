package org.sosly.villagetale.entity.ai.sensor;

import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import org.sosly.villagetale.api.IWantedItem;
import org.sosly.villagetale.data.WantedItem;
import org.sosly.villagetale.entity.MemoryModuleTypes;
import org.sosly.villagetale.entity.Villager;
import org.sosly.villagetale.helper.ItemMatcher;
import org.sosly.villagetale.helper.VillagerHelper;

public class HasResources extends Sensor<Villager> {


    public HasResources() {
        super(200);
    }

    @Override
    protected void doTick(ServerLevel level, Villager villager) {
        List<IWantedItem> requiredResources = ItemMatcher.RESOURCES.getFor(villager);

        if (requiredResources.isEmpty()) {
            return;
        }

        IWantedItem neededItem = null;
        for (IWantedItem wanted : requiredResources) {
            int count = countResources(villager, wanted);
            if (count <= wanted.getMinimum()) {
                neededItem = wanted;
                break;
            }
        }

        boolean hasExistingWant = villager.getBrain().hasMemoryValue(MemoryModuleTypes.WANTED_ITEM.get());

        if (neededItem != null && !hasExistingWant) {
            villager.getBrain().setMemoryWithExpiry(MemoryModuleTypes.WANTED_ITEM.get(), neededItem, 2400L);
            villager.getBrain().eraseMemory(MemoryModuleTypes.ALREADY_SCANNED_STORAGES.get());
            villager.getBrain().eraseMemory(MemoryModuleTypes.FOUND_ITEM.get());
            return;
        }

        if (!hasExistingWant) {
            return;
        }

        IWantedItem currentWant = villager.getBrain().getMemory(MemoryModuleTypes.WANTED_ITEM.get()).orElse(null);
        if (currentWant == null) {
            return;
        }

        int currentCount = countResources(villager, currentWant);
        if (currentCount >= currentWant.getAmount()) {
            villager.getBrain().eraseMemory(MemoryModuleTypes.WANTED_ITEM.get());
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
