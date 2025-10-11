package org.sosly.villagetale.entity.ai.sensor;

import com.google.common.collect.ImmutableSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.NotNull;
import org.sosly.villagetale.api.IVillageZone;
import org.sosly.villagetale.compat.CompatRegistry;
import org.sosly.villagetale.entity.MemoryModuleTypes;
import org.sosly.villagetale.entity.Villager;
import org.sosly.villagetale.helper.ContainerHelper;
import org.sosly.villagetale.helper.VillagesHelper;

public class HasWorkstationOutput extends Sensor<Villager> {
    @Override
    protected void doTick(@NotNull ServerLevel level, @NotNull Villager villager) {
        IVillageZone zone = VillagesHelper.getWorkplaceZone(level, villager);
        if (zone == null) {
            villager.getBrain().eraseMemory(MemoryModuleTypes.WORKSTATION_OUTPUT_READY.get());
            villager.getBrain().eraseMemory(MemoryModuleTypes.NEAREST_WORKSTATION.get());
            return;
        }

        Set<BlockPos> workstations = zone.getClaims(level.getGameTime()).keySet();
        if (workstations.isEmpty()) {
            villager.getBrain().eraseMemory(MemoryModuleTypes.WORKSTATION_OUTPUT_READY.get());
            villager.getBrain().eraseMemory(MemoryModuleTypes.NEAREST_WORKSTATION.get());
            return;
        }

        for (BlockPos workstation : workstations) {
            Block block = level.getBlockState(workstation).getBlock();
            int[] outputSlots = CompatRegistry.getRecipeManager().getOutputSlotsForBlock(block);

            if (outputSlots.length == 0) {
                continue;
            }

            for (int slot : outputSlots) {
                if (ContainerHelper.hasItemInSlot(level, workstation, slot)) {
                    villager.getBrain().setMemory(MemoryModuleTypes.WORKSTATION_OUTPUT_READY.get(), true);
                    villager.getBrain().setMemory(MemoryModuleTypes.NEAREST_WORKSTATION.get(), workstation);
                    return;
                }
            }
        }

        villager.getBrain().eraseMemory(MemoryModuleTypes.WORKSTATION_OUTPUT_READY.get());
        villager.getBrain().eraseMemory(MemoryModuleTypes.NEAREST_WORKSTATION.get());
    }

    @Override
    public @NotNull Set<MemoryModuleType<?>> requires() {
        return ImmutableSet.of();
    }
}
