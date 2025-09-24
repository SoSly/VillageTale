package org.sosly.villagetale.entity.ai.sensor;

import com.google.common.collect.ImmutableSet;
import java.util.HashSet;
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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.sosly.villagetale.api.IVillageZone;
import org.sosly.villagetale.data.Tree;
import org.sosly.villagetale.entity.MemoryModuleTypes;
import org.sosly.villagetale.entity.Villager;
import org.sosly.villagetale.helper.InventoryHelper;
import org.sosly.villagetale.helper.VillagesHelper;
import org.sosly.villagetale.zone.type.Forest;

public class IsForest extends Sensor<Villager> {
    private static final Set<BlockPos> replantedPositions = new HashSet<>();
    
    public IsForest() {
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

        if (!(zone.getType() instanceof Forest forest)) {
            return;
        }

        long gameTime = level.getGameTime();
        
        Set<Tree> trees = forest.getTrees();
        if (!trees.isEmpty()) {
            Optional<Tree> nearestTree = trees.stream()
                .filter(tree -> zone.containsPosition(tree.getBase()))
                .filter(tree -> level.getBlockState(tree.getBase()).is(BlockTags.LOGS))
                .min((t1, t2) -> {
                    double dist1 = villager.blockPosition().distSqr(t1.getBase());
                    double dist2 = villager.blockPosition().distSqr(t2.getBase());
                    return Double.compare(dist1, dist2);
                });
            
            nearestTree.ifPresent(tree -> 
                villager.getBrain().setMemoryWithExpiry(MemoryModuleTypes.NEAREST_TREE.get(), tree.getBase(), 600L)
            );
        }

        ItemStack sapling = InventoryHelper.getItem(villager, stack -> stack.is(ItemTags.SAPLINGS), zone);
        if (!sapling.isEmpty()) {
            Optional<BlockPos> replantableSpot = findReplantableSpot(level, zone, villager.blockPosition());
            replantableSpot.ifPresent(pos -> {
                villager.getBrain().setMemoryWithExpiry(MemoryModuleTypes.NEAREST_REPLANTABLE_SPOT.get(), pos, 600L);
                replantedPositions.add(pos);
            });
        }
    }

    private Optional<BlockPos> findReplantableSpot(ServerLevel level, IVillageZone zone, BlockPos villagerPos) {
        int searchRadius = 16;
        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
        
        for (int x = -searchRadius; x <= searchRadius; x++) {
            for (int z = -searchRadius; z <= searchRadius; z++) {
                for (int y = -2; y <= 2; y++) {
                    mutablePos.set(villagerPos.getX() + x, villagerPos.getY() + y, villagerPos.getZ() + z);
                    
                    if (!zone.containsPosition(mutablePos)) {
                        continue;
                    }
                    
                    if (replantedPositions.contains(mutablePos)) {
                        continue;
                    }
                    
                    if (canPlantSapling(level, mutablePos)) {
                        return Optional.of(mutablePos.immutable());
                    }
                }
            }
        }
        
        return Optional.empty();
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

    public static void clearReplantedPosition(BlockPos pos) {
        replantedPositions.remove(pos);
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