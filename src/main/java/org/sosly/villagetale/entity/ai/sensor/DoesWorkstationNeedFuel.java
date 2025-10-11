package org.sosly.villagetale.entity.ai.sensor;

import com.google.common.collect.ImmutableSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.NotNull;
import org.sosly.villagetale.api.IRecipeManager;
import org.sosly.villagetale.api.IVillageZone;
import org.sosly.villagetale.compat.CompatRegistry;
import org.sosly.villagetale.entity.MemoryModuleTypes;
import org.sosly.villagetale.entity.Villager;
import org.sosly.villagetale.helper.ContainerHelper;
import org.sosly.villagetale.helper.VillagesHelper;

public class DoesWorkstationNeedFuel extends Sensor<Villager> {
    @Override
    protected void doTick(@NotNull ServerLevel level, @NotNull Villager villager) {
        IVillageZone workZone = VillagesHelper.getWorkplaceZone(level, villager);
        if (workZone == null) {
            villager.getBrain().eraseMemory(MemoryModuleTypes.WORKSTATION_NEEDS_FUEL.get());
            return;
        }

        Map<BlockPos, Optional<UUID>> claims = workZone.getClaims(level.getGameTime());
        if (claims.isEmpty()) {
            villager.getBrain().eraseMemory(MemoryModuleTypes.WORKSTATION_NEEDS_FUEL.get());
            return;
        }

        IRecipeManager recipeManager = CompatRegistry.getRecipeManager();

        for (BlockPos workstation : claims.keySet()) {
            Block block = level.getBlockState(workstation).getBlock();

            if (!recipeManager.doesBlockRequireFuel(block)) {
                continue;
            }

            int fuelSlot = recipeManager.getFuelSlotForBlock(block).orElse(-1);
            if (fuelSlot < 0) {
                continue;
            }

            if (ContainerHelper.hasItemInSlot(level, workstation, fuelSlot)) {
                continue;
            }

            villager.getBrain().setMemory(MemoryModuleTypes.WORKSTATION_NEEDS_FUEL.get(), workstation);
            return;
        }

        villager.getBrain().eraseMemory(MemoryModuleTypes.WORKSTATION_NEEDS_FUEL.get());
    }

    @Override
    public @NotNull Set<MemoryModuleType<?>> requires() {
        return ImmutableSet.of(
            MemoryModuleTypes.WORKSTATION_NEEDS_FUEL.get()
        );
    }
}
