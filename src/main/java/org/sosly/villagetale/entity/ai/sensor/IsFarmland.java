package org.sosly.villagetale.entity.ai.sensor;

import com.google.common.collect.ImmutableSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.sosly.villagetale.api.IVillageZone;
import org.sosly.villagetale.entity.MemoryModuleTypes;
import org.sosly.villagetale.entity.Villager;
import org.sosly.villagetale.helper.InventoryHelper;
import org.sosly.villagetale.helper.VillagesHelper;
import org.sosly.villagetale.zone.type.Farmland;

public class IsFarmland extends Sensor<Villager> {
    public IsFarmland() {
        super(100);
    }

    @Override
    protected void doTick(ServerLevel level, Villager villager) {
        UUID workplaceId = villager.getBrain().getMemory(MemoryModuleTypes.WORK_ZONE.get()).orElse(null);
        if (workplaceId == null) {
            return;
        }

        IVillageZone zone = VillagesHelper.getZoneById(level, villager.getVillage().get(), workplaceId);
        if (zone == null) {
            return;
        }

        if (!zone.containsPosition(villager.blockPosition())) {
            return;
        }

        long gameTime = level.getGameTime();
        List<BlockPos> claims = zone.getAvailableClaims(gameTime, Optional.empty());
        Optional<BlockPos> harvestable = claims
            .stream()
            .filter(pos -> Farmland.isHarvestableBlock(level, pos))
            .findAny();
        harvestable.ifPresent(blockPos -> villager.getBrain().setMemoryWithExpiry(MemoryModuleTypes.NEAREST_HARVESTABLE_CROP.get(), blockPos, 600L));

        ItemStack seeds = InventoryHelper.getSeeds(villager, zone);
        if (!seeds.isEmpty()) {
            Optional<BlockPos> plantable = claims
                .stream()
                .filter(pos -> Farmland.isPlantableBlock(level, pos, seeds))
                .findAny();
            plantable.ifPresent(blockPos -> villager.getBrain().setMemoryWithExpiry(MemoryModuleTypes.NEAREST_EMPTY_FARMLAND.get(), blockPos, 600L));
        }

        Optional<BlockPos> tillable = claims
            .stream()
            .filter(pos -> Farmland.isTillableBlock(level, pos))
            .findAny();
        tillable.ifPresent(blockPos -> villager.getBrain().setMemoryWithExpiry(MemoryModuleTypes.NEAREST_TILLABLE_SOIL.get(), blockPos, 600L));
    }

    @Override
    public @NotNull Set<MemoryModuleType<?>> requires() {
        return ImmutableSet.of(
            MemoryModuleTypes.WORK_ZONE.get(),
            MemoryModuleTypes.NEAREST_EMPTY_FARMLAND.get(),
            MemoryModuleTypes.NEAREST_HARVESTABLE_CROP.get(),
            MemoryModuleTypes.NEAREST_TILLABLE_SOIL.get()
        );
    }
}
