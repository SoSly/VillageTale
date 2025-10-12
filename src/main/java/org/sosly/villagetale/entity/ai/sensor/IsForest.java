package org.sosly.villagetale.entity.ai.sensor;

import com.google.common.collect.ImmutableSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.sosly.villagetale.api.IVillageZone;
import org.sosly.villagetale.api.IWantedItem;
import org.sosly.villagetale.config.CommonConfig;
import org.sosly.villagetale.data.Tree;
import org.sosly.villagetale.entity.MemoryModuleTypes;
import org.sosly.villagetale.entity.Villager;
import org.sosly.villagetale.helper.InventoryHelper;
import org.sosly.villagetale.helper.TreeHelper;
import org.sosly.villagetale.helper.VillagesHelper;
import org.sosly.villagetale.zone.type.Forest;

public class IsForest extends Sensor<Villager> {
    public IsForest() {
        super(100);
    }

    @Override
    protected void doTick(@NotNull ServerLevel level, Villager villager) {
        if (villager.getVillage().isEmpty() || !villager.getBrain().hasMemoryValue(MemoryModuleTypes.WORK_ZONE.get())) {
            return;
        }

        UUID workplaceId = villager.getBrain().getMemory(MemoryModuleTypes.WORK_ZONE.get()).get();
        IVillageZone zone = VillagesHelper.getZoneById(level, villager.getVillage().get(), workplaceId);
        if (zone == null) {
            return;
        }

        if (!zone.containsPosition(villager.blockPosition())) {
            return;
        }

        if (!(zone.getType() instanceof Forest)) {
            return;
        }

        findNearestReplantableSpot(level, villager, zone);
        findNearestTree(level, villager);
    }

    private void findNearestTree(ServerLevel level, Villager villager) {
        List<IWantedItem> tools = villager.getProfession().getTools();
        if (tools.isEmpty()) {
            return;
        }

        ItemStack tool = InventoryHelper.getItem(villager, stack -> tools.get(0).getMatcher().test(stack));
        if (tool.isEmpty()) {
            return;
        }

        Optional<Tree> tree = TreeHelper.findNearestTree(level, villager);
        if (tree.isEmpty()) {
            return;
        }

        villager.getBrain().setMemoryWithExpiry(MemoryModuleTypes.NEAREST_TREE.get(), tree.get(), 600L);
    }

    private void findNearestReplantableSpot(ServerLevel level, Villager villager, IVillageZone zone) {
        ItemStack sapling = InventoryHelper.getItem(villager, stack -> stack.is(ItemTags.SAPLINGS), zone);
        if (sapling.isEmpty()) {
            return;
        }

        boolean needs2x2 = needs2x2Configuration(sapling);
        BlockPos villagerPos = villager.blockPosition();
        int scanRadius = (int) CommonConfig.scanRadius;
        BlockPos nearestSpot = null;
        double nearestDistance = Double.MAX_VALUE;

        for (int x = -scanRadius; x <= scanRadius; x++) {
            for (int z = -scanRadius; z <= scanRadius; z++) {
                BlockPos pos = villagerPos.offset(x, 0, z);

                if (!zone.containsPosition(pos)) {
                    continue;
                }

                if (needs2x2) {
                    if (!canPlace2x2Sapling(level, pos, zone) || !has2x2Spacing(level, pos)) {
                        continue;
                    }
                } else {
                    if (!canPlantSapling(level, pos) || !hasProperSpacing(level, pos)) {
                        continue;
                    }
                }

                double distance = villagerPos.distSqr(pos);
                if (distance < nearestDistance) {
                    nearestDistance = distance;
                    nearestSpot = pos;
                }
            }
        }

        if (nearestSpot != null) {
            villager.getBrain().setMemoryWithExpiry(MemoryModuleTypes.NEAREST_REPLANTABLE_SPOT.get(), nearestSpot, 600L);
        }
    }

    private boolean canPlantSapling(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!state.isAir()) {
            return false;
        }

        BlockPos below = pos.below();
        BlockState belowState = level.getBlockState(below);

        return belowState.is(BlockTags.DIRT) ||
               belowState.is(Blocks.GRASS_BLOCK) ||
               belowState.is(Blocks.PODZOL) ||
               belowState.is(Blocks.MYCELIUM);
    }

    private boolean canPlace2x2Sapling(ServerLevel level, BlockPos pos, IVillageZone zone) {
        BlockPos north = pos.north();
        BlockPos east = pos.east();
        BlockPos northeast = pos.north().east();

        if (!zone.containsPosition(north) || !zone.containsPosition(east) || !zone.containsPosition(northeast)) {
            return false;
        }

        return canPlantSapling(level, pos) &&
               canPlantSapling(level, north) &&
               canPlantSapling(level, east) &&
               canPlantSapling(level, northeast);
    }

    private boolean hasProperSpacing(ServerLevel level, BlockPos pos) {
        int minSpacing = 3;

        for (int dx = -minSpacing; dx <= minSpacing; dx++) {
            for (int dz = -minSpacing; dz <= minSpacing; dz++) {
                if (dx == 0 && dz == 0) {
                    continue;
                }

                BlockPos nearbyPos = pos.offset(dx, 0, dz);
                BlockState nearbyState = level.getBlockState(nearbyPos);

                if (!nearbyState.is(BlockTags.SAPLINGS) && !nearbyState.is(BlockTags.LOGS)) {
                    continue;
                }

                double distance = Math.sqrt(dx * dx + dz * dz);
                if (distance < minSpacing) {
                    return false;
                }
            }
        }

        return true;
    }

    private boolean has2x2Spacing(ServerLevel level, BlockPos pos) {
        int checkRadius = 4;

        for (int dx = -checkRadius; dx <= checkRadius + 1; dx++) {
            for (int dz = -checkRadius; dz <= checkRadius + 1; dz++) {
                if (dx >= 0 && dx <= 1 && dz >= 0 && dz <= 1) {
                    continue;
                }

                BlockPos checkPos = pos.offset(dx, 0, dz);
                BlockState state = level.getBlockState(checkPos);

                if (!state.is(BlockTags.SAPLINGS) && !state.is(BlockTags.LOGS)) {
                    continue;
                }

                int closestX = Math.max(0, Math.min(1, dx));
                int closestZ = Math.max(0, Math.min(1, dz));
                int distX = Math.abs(dx - closestX);
                int distZ = Math.abs(dz - closestZ);
                double distance = Math.sqrt(distX * distX + distZ * distZ);

                if (distance < 3) {
                    return false;
                }
            }
        }

        return true;
    }

    private boolean needs2x2Configuration(ItemStack sapling) {
        return sapling.is(Items.JUNGLE_SAPLING) ||
               sapling.is(Items.SPRUCE_SAPLING) ||
               sapling.is(Items.DARK_OAK_SAPLING);
    }

    @Override
    public @NotNull Set<MemoryModuleType<?>> requires() {
        return ImmutableSet.of(
            MemoryModuleTypes.WORK_ZONE.get(),
            MemoryModuleTypes.NEAREST_TREE.get(),
            MemoryModuleTypes.NEAREST_REPLANTABLE_SPOT.get()
        );
    }
}
