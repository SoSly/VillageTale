package org.sosly.villagetale.entity.ai.sensor;

import com.google.common.collect.ImmutableSet;
import java.util.Optional;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.sensing.Sensor;
import org.jetbrains.annotations.NotNull;
import org.sosly.villagetale.entity.MemoryModuleTypes;
import org.sosly.villagetale.entity.Villager;
import org.sosly.villagetale.helper.VillagerHelper;

public class WhatShouldBeCrafted extends Sensor<Villager> {
    protected void doTick(@NotNull ServerLevel level, @NotNull Villager villager) {
        if (villager.getBrain().hasMemoryValue(MemoryModuleTypes.CURRENT_RECIPE.get())) {
            return;
        }

        Optional<ResourceLocation> recipe = VillagerHelper.getRandomRecipe(level, villager);
        if (recipe.isEmpty()) {
            return;
        }

        if (level.getRecipeManager().byKey(recipe.get()).isEmpty()) {
            return;
        }

        villager.getBrain().setMemoryWithExpiry(MemoryModuleTypes.CURRENT_RECIPE.get(), recipe.get(), 2400L);
    }

    @Override
    public @NotNull Set<MemoryModuleType<?>> requires() {
        return ImmutableSet.of(
            MemoryModuleTypes.CURRENT_RECIPE.get()
        );
    }
}
