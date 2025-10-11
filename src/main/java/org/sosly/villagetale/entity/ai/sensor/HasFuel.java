package org.sosly.villagetale.entity.ai.sensor;

import com.google.common.collect.ImmutableSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import org.sosly.villagetale.api.IWantedItem;
import org.sosly.villagetale.compat.CompatRegistry;
import org.sosly.villagetale.data.ItemOrTagMatcher;
import org.sosly.villagetale.data.WantedItem;
import org.sosly.villagetale.entity.MemoryModuleTypes;
import org.sosly.villagetale.entity.Villager;

public class HasFuel extends Sensor<Villager> {
    private static final int MINIMUM_FUEL = 0;
    private static final int TARGET_FUEL = 3;

    public HasFuel() {
        super(200);
    }

    @Override
    protected void doTick(ServerLevel level, Villager villager) {
        BlockPos workstation = villager.getBrain().getMemory(MemoryModuleTypes.WORKSTATION_NEEDS_FUEL.get()).orElse(null);
        if (workstation == null) {
            return;
        }

        Block block = level.getBlockState(workstation).getBlock();
        ItemOrTagMatcher fuelMatcher = CompatRegistry.getRecipeManager().getFuelItemsForBlock(block).orElse(null);
        if (fuelMatcher == null) {
            return;
        }

        int currentFuel = countFuel(villager, fuelMatcher);
        if (currentFuel > MINIMUM_FUEL) {
            return;
        }

        IWantedItem fuelWanted = new WantedItem(fuelMatcher::matches, TARGET_FUEL, MINIMUM_FUEL);
        villager.getBrain().setMemoryWithExpiry(MemoryModuleTypes.WANTED_ITEM.get(), fuelWanted, 2400L);
        villager.getBrain().eraseMemory(MemoryModuleTypes.ALREADY_SCANNED_STORAGES.get());
        villager.getBrain().eraseMemory(MemoryModuleTypes.FOUND_ITEM.get());
    }

    private int countFuel(Villager villager, ItemOrTagMatcher fuelMatcher) {
        int count = 0;
        for (int i = 0; i < villager.getInventory().getContainerSize(); i++) {
            ItemStack stack = villager.getInventory().getItem(i);
            if (!stack.isEmpty() && fuelMatcher.matches(stack)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    @Override
    public Set<MemoryModuleType<?>> requires() {
        return ImmutableSet.of(
            MemoryModuleTypes.WORKSTATION_NEEDS_FUEL.get(),
            MemoryModuleTypes.WANTED_ITEM.get(),
            MemoryModuleTypes.ALREADY_SCANNED_STORAGES.get(),
            MemoryModuleTypes.FOUND_ITEM.get()
        );
    }
}
