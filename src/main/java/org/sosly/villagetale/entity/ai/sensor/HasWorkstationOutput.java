package org.sosly.villagetale.entity.ai.sensor;

import com.google.common.collect.ImmutableSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.item.crafting.Recipe;
import org.jetbrains.annotations.NotNull;
import org.sosly.villagetale.compat.CompatRegistry;
import org.sosly.villagetale.data.CraftingMethod;
import org.sosly.villagetale.entity.MemoryModuleTypes;
import org.sosly.villagetale.entity.Villager;
import org.sosly.villagetale.helper.ContainerHelper;
import org.sosly.villagetale.helper.VillagerHelper;

public class HasWorkstationOutput extends Sensor<Villager> {
    @Override
    protected void doTick(@NotNull ServerLevel level, @NotNull Villager villager) {
        BlockPos workstation = villager.getBrain().getMemory(MemoryModuleTypes.NEAREST_WORKSTATION.get()).orElse(null);
        if (workstation == null) {
            villager.getBrain().eraseMemory(MemoryModuleTypes.WORKSTATION_OUTPUT_READY.get());
            return;
        }

        Recipe<?> recipe = VillagerHelper.getCurrentRecipe(level, villager);
        if (recipe == null) {
            villager.getBrain().eraseMemory(MemoryModuleTypes.WORKSTATION_OUTPUT_READY.get());
            return;
        }

        CraftingMethod method = CompatRegistry.getRecipeManager().getCraftingMethod(recipe);
        if (method != CraftingMethod.CONTAINER) {
            villager.getBrain().eraseMemory(MemoryModuleTypes.WORKSTATION_OUTPUT_READY.get());
            return;
        }

        int[] outputSlots = CompatRegistry.getRecipeManager().getOutputSlots(recipe);
        if (outputSlots.length == 0) {
            villager.getBrain().eraseMemory(MemoryModuleTypes.WORKSTATION_OUTPUT_READY.get());
            return;
        }

        boolean hasOutput = false;
        for (int slot : outputSlots) {
            if (ContainerHelper.hasItemInSlot(level, workstation, slot)) {
                hasOutput = true;
                break;
            }
        }

        if (hasOutput) {
            villager.getBrain().setMemory(MemoryModuleTypes.WORKSTATION_OUTPUT_READY.get(), true);
        } else {
            villager.getBrain().eraseMemory(MemoryModuleTypes.WORKSTATION_OUTPUT_READY.get());
        }
    }

    @Override
    public @NotNull Set<MemoryModuleType<?>> requires() {
        return ImmutableSet.of(
            MemoryModuleTypes.NEAREST_WORKSTATION.get(),
            MemoryModuleTypes.CURRENT_RECIPE.get()
        );
    }
}
