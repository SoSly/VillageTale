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
import org.sosly.villagetale.helper.ItemMatcher;

import java.util.Set;

public class HasTool extends Sensor<Villager> {

    public HasTool() {
        super(200);
    }

    @Override
    protected void doTick(ServerLevel level, Villager villager) {
        IWantedItem requiredTool = villager.getProfession().getTool().orElse(null);

        if (requiredTool == null || requiredTool == IWantedItem.EMPTY) {
            return;
        }

        boolean hasTool = hasTool(villager, requiredTool);
        boolean hasExistingWant = villager.getBrain().hasMemoryValue(MemoryModuleTypes.WANTED_ITEM.get());

        if (!hasTool && !hasExistingWant) {
            villager.getBrain().setMemory(MemoryModuleTypes.WANTED_ITEM.get(), ItemMatcher.PROFESSION_TOOL.getFor(villager));
            villager.getBrain().eraseMemory(MemoryModuleTypes.ALREADY_SCANNED_STORAGES.get());
            villager.getBrain().eraseMemory(MemoryModuleTypes.FOUND_ITEM.get());

            if (VillageTale.LOGGER.isDebugEnabled()) {
                VillageTale.LOGGER.debug("HasTool set WANTED_ITEM to PROFESSION_TOOL for villager {}", villager.getId());
            }
            return;
        }

        if (!hasTool || !hasExistingWant) {
            return;
        }

        var currentWant = villager.getBrain().getMemory(MemoryModuleTypes.WANTED_ITEM.get()).orElse(null);
        if (currentWant == null || !currentWant.equals(ItemMatcher.PROFESSION_TOOL.getFor(villager))) {
            return;
        }

        villager.getBrain().eraseMemory(MemoryModuleTypes.WANTED_ITEM.get());

        if (VillageTale.LOGGER.isDebugEnabled()) {
            VillageTale.LOGGER.debug("HasTool cleared WANTED_ITEM for villager {}", villager.getId());
        }
    }

    private boolean hasTool(Villager villager, IWantedItem requiredTool) {
        for (int i = 0; i < villager.getInventory().getContainerSize(); i++) {
            ItemStack stack = villager.getInventory().getItem(i);
            if (!stack.isEmpty() && requiredTool.getMatcher().test(stack)) {
                return true;
            }
        }
        return false;
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
