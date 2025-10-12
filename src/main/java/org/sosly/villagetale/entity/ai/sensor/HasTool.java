package org.sosly.villagetale.entity.ai.sensor;

import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.item.ItemStack;
import org.sosly.villagetale.VillageTale;
import org.sosly.villagetale.api.IWantedItem;
import org.sosly.villagetale.entity.MemoryModuleTypes;
import org.sosly.villagetale.entity.Villager;

public class HasTool extends Sensor<Villager> {

    public HasTool() {
        super(200);
    }

    @Override
    protected void doTick(ServerLevel level, Villager villager) {
        List<IWantedItem> requiredTools = villager.getProfession().getTools();

        if (requiredTools.isEmpty()) {
            return;
        }

        List<IWantedItem> missingTools = findMissingTools(villager, requiredTools);
        boolean hasExistingWant = villager.getBrain().hasMemoryValue(MemoryModuleTypes.WANTED_ITEM.get());

        if (!missingTools.isEmpty() && !hasExistingWant) {
            villager.getBrain().setMemoryWithExpiry(MemoryModuleTypes.WANTED_ITEM.get(), missingTools.get(0), 2400L);
            villager.getBrain().eraseMemory(MemoryModuleTypes.ALREADY_SCANNED_STORAGES.get());
            villager.getBrain().eraseMemory(MemoryModuleTypes.FOUND_ITEM.get());

            VillageTale.LOGGER.debug("HasTool set WANTED_ITEM to PROFESSION_TOOL for villager {}", villager.getId());
            return;
        }

        if (missingTools.isEmpty() && hasExistingWant) {
            IWantedItem currentWant = villager.getBrain().getMemory(MemoryModuleTypes.WANTED_ITEM.get()).orElse(null);
            if (currentWant != null && isToolWant(currentWant, requiredTools)) {
                villager.getBrain().eraseMemory(MemoryModuleTypes.WANTED_ITEM.get());
                VillageTale.LOGGER.debug("HasTool cleared WANTED_ITEM for villager {}", villager.getId());
            }
        }
    }

    private List<IWantedItem> findMissingTools(Villager villager, List<IWantedItem> requiredTools) {
        List<IWantedItem> missing = new ArrayList<>();
        for (IWantedItem tool : requiredTools) {
            if (!hasTool(villager, tool)) {
                missing.add(tool);
            }
        }
        return missing;
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

    private boolean isToolWant(IWantedItem currentWant, List<IWantedItem> requiredTools) {
        for (IWantedItem tool : requiredTools) {
            if (currentWant.equals(tool)) {
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
