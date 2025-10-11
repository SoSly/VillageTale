package org.sosly.villagetale.entity.ai.sensor;

import com.google.common.collect.ImmutableSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
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

        Optional<UUID> village = villager.getVillage();
        if (village.isEmpty()) {
            return;
        }

        IVillageZone zone = VillagesHelper.getZoneById(level, village.get(), workplaceId);
        if (zone == null) {
            return;
        }

        if (!zone.containsPosition(villager.blockPosition())) {
            return;
        }

        long gameTime = level.getGameTime();
        List<BlockPos> claims = zone.getAvailableClaims(gameTime, Optional.empty());

        List<BlockPos> harvestable = claims
            .stream()
            .filter(pos -> Farmland.isHarvestableBlock(level, pos))
            .toList();
        if (!harvestable.isEmpty()) {
            BlockPos selected = harvestable.get(villager.getRandom().nextInt(harvestable.size()));
            villager.getBrain().setMemoryWithExpiry(MemoryModuleTypes.NEAREST_HARVESTABLE_CROP.get(), selected, 600L);
        }

        ItemStack seeds = InventoryHelper.getItem(villager, stack -> stack.is(ItemTags.VILLAGER_PLANTABLE_SEEDS), zone);
        if (!seeds.isEmpty()) {
            List<BlockPos> plantable = claims
                .stream()
                .filter(pos -> Farmland.isPlantableBlock(level, pos, seeds))
                .toList();
            if (!plantable.isEmpty()) {
                BlockPos selected = plantable.get(villager.getRandom().nextInt(plantable.size()));
                villager.getBrain().setMemoryWithExpiry(MemoryModuleTypes.NEAREST_EMPTY_FARMLAND.get(), selected, 600L);
            }
        }

        List<BlockPos> tillable = claims
            .stream()
            .filter(pos -> Farmland.isTillableBlock(level, pos))
            .toList();
        if (!tillable.isEmpty()) {
            BlockPos selected = tillable.get(villager.getRandom().nextInt(tillable.size()));
            villager.getBrain().setMemoryWithExpiry(MemoryModuleTypes.NEAREST_TILLABLE_SOIL.get(), selected, 600L);
        }
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
