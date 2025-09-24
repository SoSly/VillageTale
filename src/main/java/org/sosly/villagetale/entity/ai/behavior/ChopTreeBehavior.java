package org.sosly.villagetale.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.sosly.villagetale.api.IVillageZone;
import org.sosly.villagetale.config.CommonConfig;
import org.sosly.villagetale.entity.MemoryModuleTypes;
import org.sosly.villagetale.entity.Villager;
import org.sosly.villagetale.helper.InventoryHelper;
import org.sosly.villagetale.helper.VillagesHelper;
import org.sosly.villagetale.network.NetworkHandler;
import org.sosly.villagetale.zone.type.Forest;

public class ChopTreeBehavior extends Behavior<Villager> {
    private static final int CHOP_DURATION_PER_BLOCK = 20;
    private static final int BEHAVIOR_DURATION = 600;
    private static final float WORK_EXHAUSTION = 1.0f;
    private static final int MAX_TREE_HEIGHT = 30;

    private boolean claimed;
    private BlockPos treeBase;
    private int chopTicks;
    private ItemStack axe = ItemStack.EMPTY;
    private IVillageZone workplace;
    private Deque<BlockPos> blocksToChop = new ArrayDeque<>();
    private BlockPos currentBlock;

    public ChopTreeBehavior() {
        super(ImmutableMap.of(
                MemoryModuleTypes.WORK_ZONE.get(), MemoryStatus.VALUE_PRESENT,
                MemoryModuleTypes.NEAREST_TREE.get(), MemoryStatus.VALUE_PRESENT,
                MemoryModuleTypes.BUSY.get(), MemoryStatus.VALUE_ABSENT
        ), BEHAVIOR_DURATION);
    }

    @Override
    protected boolean checkExtraStartConditions(@NotNull ServerLevel level, @NotNull Villager villager) {
        ItemStack axe = InventoryHelper.getItem(villager, stack -> stack.is(ItemTags.AXES));
        if (axe.isEmpty()) {
            return false;
        }

        IVillageZone zone = VillagesHelper.getWorkplaceZone(level, villager);
        if (zone == null) {
            return false;
        }

        if (!zone.containsPosition(villager.blockPosition())) {
            return false;
        }

        this.axe = axe;
        this.workplace = zone;
        return true;
    }

    @Override
    protected void start(@NotNull ServerLevel level, Villager villager, long gameTime) {
        treeBase = villager.getBrain().getMemory(MemoryModuleTypes.NEAREST_TREE.get()).orElse(null);
        if (treeBase == null) {
            return;
        }

        claimed = workplace.claim(treeBase, villager.getUUID(), BEHAVIOR_DURATION, gameTime);
        if (!claimed) {
            return;
        }

        findTreeBlocks(level, treeBase);
        if (blocksToChop.isEmpty()) {
            claimed = false;
            return;
        }

        villager.getBrain().setMemoryWithExpiry(MemoryModuleTypes.BUSY.get(), true, BEHAVIOR_DURATION);
        villager.setItemInHand(InteractionHand.MAIN_HAND, axe);
        NetworkHandler.syncEquipmentToNearbyPlayers(villager, InteractionHand.MAIN_HAND, axe);

        if (!villager.blockPosition().closerThan(treeBase, CommonConfig.interactionDistance)) {
            villager.getBrain().setMemoryWithExpiry(MemoryModuleType.WALK_TARGET,
                new WalkTarget(treeBase, 0.5F, 1), 200L);
        }
    }

    private void findTreeBlocks(ServerLevel level, BlockPos basePos) {
        Set<BlockPos> visited = new HashSet<>();
        Deque<BlockPos> toVisit = new ArrayDeque<>();
        toVisit.add(basePos);

        while (!toVisit.isEmpty() && visited.size() < 500) {
            BlockPos current = toVisit.poll();
            if (!visited.add(current)) {
                continue;
            }

            BlockState state = level.getBlockState(current);
            if (state.is(BlockTags.LOGS)) {
                blocksToChop.addFirst(current);
                
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dy = 0; dy <= 1; dy++) {
                        for (int dz = -1; dz <= 1; dz++) {
                            if (dx == 0 && dy == 0 && dz == 0) continue;
                            
                            BlockPos neighbor = current.offset(dx, dy, dz);
                            if (!visited.contains(neighbor) && 
                                neighbor.getY() - basePos.getY() < MAX_TREE_HEIGHT) {
                                toVisit.add(neighbor);
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    protected void stop(@NotNull ServerLevel level, Villager villager, long gameTime) {
        villager.getBrain().eraseMemory(MemoryModuleTypes.BUSY.get());
        villager.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        NetworkHandler.syncEquipmentToNearbyPlayers(villager, InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        villager.getBrain().eraseMemory(MemoryModuleTypes.NEAREST_TREE.get());

        if (workplace != null && workplace.getType() instanceof Forest forest) {
            forest.markDirty(treeBase);
        }

        this.treeBase = null;
        this.chopTicks = 0;
        this.axe = ItemStack.EMPTY;
        this.workplace = null;
        this.blocksToChop.clear();
        this.currentBlock = null;
    }

    @Override
    protected boolean canStillUse(@NotNull ServerLevel level, @NotNull Villager villager, long gameTime) {
        return claimed && !blocksToChop.isEmpty();
    }

    @Override
    protected void tick(@NotNull ServerLevel level, @NotNull Villager villager, long gameTime) {
        if (!claimed || blocksToChop.isEmpty()) {
            return;
        }

        if (currentBlock == null) {
            currentBlock = blocksToChop.poll();
            chopTicks = 0;
        }

        if (currentBlock == null) {
            claimed = false;
            return;
        }

        if (!villager.blockPosition().closerThan(currentBlock, CommonConfig.interactionDistance + 2)) {
            villager.getBrain().setMemoryWithExpiry(MemoryModuleType.WALK_TARGET,
                new WalkTarget(currentBlock, 0.5F, 1), 200L);
            return;
        }

        villager.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
        
        if (chopTicks++ < CHOP_DURATION_PER_BLOCK) {
            if (chopTicks % 5 == 0) {
                villager.getLookControl().setLookAt(
                    currentBlock.getX() + 0.5,
                    currentBlock.getY() + 0.5,
                    currentBlock.getZ() + 0.5
                );
                villager.swing(InteractionHand.MAIN_HAND);
            }
            return;
        }

        if (!level.getBlockState(currentBlock).is(BlockTags.LOGS)) {
            currentBlock = null;
            return;
        }

        level.destroyBlock(currentBlock, true);
        level.playSound(null, currentBlock, SoundEvents.WOOD_BREAK, SoundSource.BLOCKS, 1.0F, 1.0F);
        villager.getFoodData().addExhaustion(WORK_EXHAUSTION);

        axe.setDamageValue(axe.getDamageValue() + 1);
        if (axe.getDamageValue() >= axe.getMaxDamage()) {
            axe.shrink(1);
            level.playSound(null, currentBlock, SoundEvents.ITEM_BREAK, SoundSource.NEUTRAL, 1.0F, 1.0F);
            claimed = false;
        }

        currentBlock = null;
        
        if (blocksToChop.isEmpty()) {
            villager.getBrain().eraseMemory(MemoryModuleTypes.NEAREST_TREE.get());
            claimed = false;
        }
    }
}