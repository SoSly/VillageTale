package org.sosly.villagetale.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
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
import org.jetbrains.annotations.NotNull;
import org.sosly.villagetale.api.IVillageZone;
import org.sosly.villagetale.config.CommonConfig;
import org.sosly.villagetale.entity.MemoryModuleTypes;
import org.sosly.villagetale.entity.Villager;
import org.sosly.villagetale.helper.InventoryHelper;
import org.sosly.villagetale.helper.VillagesHelper;
import org.sosly.villagetale.network.NetworkHandler;

public class ChopTreeBehavior extends Behavior<Villager> {
    private static final int CHOP_DURATION = 40;
    private static final int BEHAVIOR_DURATION = 100;
    private static final float WORK_EXHAUSTION = 0.5f;

    private boolean claimed;
    private BlockPos targetLog;
    private int chopTicks;
    private ItemStack axe = ItemStack.EMPTY;
    private IVillageZone workplace;

    public ChopTreeBehavior() {
        super(ImmutableMap.of(
                MemoryModuleTypes.WORK_ZONE.get(), MemoryStatus.VALUE_PRESENT,
                MemoryModuleTypes.NEAREST_LOG.get(), MemoryStatus.VALUE_PRESENT,
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
        targetLog = villager.getBrain().getMemory(MemoryModuleTypes.NEAREST_LOG.get()).orElse(null);
        if (targetLog == null) {
            return;
        }

        if (!level.getBlockState(targetLog).is(BlockTags.LOGS)) {
            villager.getBrain().eraseMemory(MemoryModuleTypes.NEAREST_LOG.get());
            return;
        }

        claimed = workplace.claim(targetLog, villager.getUUID(), BEHAVIOR_DURATION, gameTime);
        if (!claimed) {
            return;
        }

        villager.getBrain().setMemoryWithExpiry(MemoryModuleTypes.BUSY.get(), true, BEHAVIOR_DURATION);
        villager.setItemInHand(InteractionHand.MAIN_HAND, axe);
        NetworkHandler.syncEquipmentToNearbyPlayers(villager, InteractionHand.MAIN_HAND, axe);

        if (!villager.blockPosition().closerThan(targetLog, CommonConfig.interactionDistance)) {
            villager.getBrain().setMemoryWithExpiry(MemoryModuleType.WALK_TARGET,
                new WalkTarget(targetLog, 0.5F, 1), 200L);
        }
    }

    @Override
    protected void stop(@NotNull ServerLevel level, Villager villager, long gameTime) {
        villager.getBrain().eraseMemory(MemoryModuleTypes.BUSY.get());
        villager.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        NetworkHandler.syncEquipmentToNearbyPlayers(villager, InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        villager.getBrain().eraseMemory(MemoryModuleTypes.NEAREST_LOG.get());

        if (targetLog != null) {
            level.destroyBlockProgress(villager.getId(), targetLog, -1);
        }

        if (claimed && targetLog != null) {
            workplace.release(targetLog);
        }

        this.targetLog = null;
        this.chopTicks = 0;
        this.axe = ItemStack.EMPTY;
        this.workplace = null;
        this.claimed = false;
    }

    @Override
    protected boolean canStillUse(@NotNull ServerLevel level, @NotNull Villager villager, long gameTime) {
        return claimed && targetLog != null && level.getBlockState(targetLog).is(BlockTags.LOGS);
    }

    @Override
    protected void tick(@NotNull ServerLevel level, @NotNull Villager villager, long gameTime) {
        if (!claimed || targetLog == null) {
            return;
        }

        if (!villager.blockPosition().closerThan(targetLog, CommonConfig.interactionDistance)) {
            villager.getBrain().setMemoryWithExpiry(MemoryModuleType.WALK_TARGET,
                new WalkTarget(targetLog, 0.5F, 0), 200L);
            return;
        }

        villager.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);

        if (chopTicks++ < CHOP_DURATION) {
            if (chopTicks % 5 == 0) {
                villager.getLookControl().setLookAt(
                    targetLog.getX() + 0.5,
                    targetLog.getY() + 0.5,
                    targetLog.getZ() + 0.5
                );
                villager.swing(InteractionHand.MAIN_HAND);
            }
            
            int progress = (int)((chopTicks / (float)CHOP_DURATION) * 10);
            level.destroyBlockProgress(villager.getId(), targetLog, progress);
            return;
        }

        level.destroyBlockProgress(villager.getId(), targetLog, -1);
        level.destroyBlock(targetLog, true);
        level.playSound(null, targetLog, SoundEvents.WOOD_BREAK, SoundSource.BLOCKS, 1.0F, 1.0F);
        villager.getFoodData().addExhaustion(WORK_EXHAUSTION);

        axe.setDamageValue(axe.getDamageValue() + 1);
        if (axe.getDamageValue() >= axe.getMaxDamage()) {
            axe.shrink(1);
            level.playSound(null, targetLog, SoundEvents.ITEM_BREAK, SoundSource.NEUTRAL, 1.0F, 1.0F);
        }

        villager.getBrain().eraseMemory(MemoryModuleTypes.NEAREST_LOG.get());
        claimed = false;
    }
}