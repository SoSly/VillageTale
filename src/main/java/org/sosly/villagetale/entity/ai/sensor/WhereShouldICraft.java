package org.sosly.villagetale.entity.ai.sensor;

import com.google.common.collect.ImmutableSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.NotNull;
import org.sosly.villagetale.api.IVillageZone;
import org.sosly.villagetale.compat.CompatRegistry;
import org.sosly.villagetale.entity.MemoryModuleTypes;
import org.sosly.villagetale.entity.Villager;
import org.sosly.villagetale.helper.VillagerHelper;
import org.sosly.villagetale.helper.VillagesHelper;

public class WhereShouldICraft extends Sensor<Villager> {
    protected void doTick(@NotNull ServerLevel level, @NotNull Villager villager) {
        Recipe<?> recipe = VillagerHelper.getCurrentRecipe(level, villager);
        if (recipe == null) {
            return;
        }

        Block craftingStation = CompatRegistry.getRecipeManager()
                .getCraftingBlock(recipe).orElse(null);
        if (craftingStation == null) {
            return;
        }

        IVillageZone zone = VillagesHelper.getWorkplaceZone(level, villager);
        if (zone == null) {
            return;
        }

        if (!zone.containsPosition(villager.blockPosition())) {
            return;
        }

        List<BlockPos> stations = zone.getAvailableClaims(level.getGameTime(),
                Optional.of(state -> state.getBlock().equals(craftingStation)));
        if (stations.isEmpty()) {
            return;
        }

        int stationPicker = level.getRandom().nextInt(stations.size());
        villager.getBrain().setMemoryWithExpiry(MemoryModuleTypes.NEAREST_WORKSTATION.get(),
                stations.get(stationPicker), 600L);
    }

    @Override
    public @NotNull Set<MemoryModuleType<?>> requires() {
        return ImmutableSet.of(
            MemoryModuleTypes.NEAREST_WORKSTATION.get()
        );
    }
}
