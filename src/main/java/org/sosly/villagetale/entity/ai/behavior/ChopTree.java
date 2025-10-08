package org.sosly.villagetale.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.sosly.villagetale.api.IVillageZone;
import org.sosly.villagetale.config.CommonConfig;
import org.sosly.villagetale.data.Tree;
import org.sosly.villagetale.entity.MemoryModuleTypes;
import org.sosly.villagetale.entity.Villager;
import org.sosly.villagetale.helper.InventoryHelper;
import org.sosly.villagetale.helper.VillagesHelper;
import org.sosly.villagetale.network.packets.clientbound.VillagerEquipmentSync;

public class ChopTree extends Behavior<Villager> {
    private static final int MINIMUM_DURATION = 40;
    private static final int BEHAVIOR_DURATION = 300;
    private static final float WORK_EXHAUSTION = 0.5f;

    private boolean claimed;
    private Tree tree;
    private List<BlockPos> logs;
    private int chopTicks;
    private int chopDuration;
    private ItemStack axe = ItemStack.EMPTY;
    private IVillageZone workplace;

    public ChopTree() {
        super(ImmutableMap.of(
                MemoryModuleTypes.WORK_ZONE.get(), MemoryStatus.VALUE_PRESENT,
                MemoryModuleTypes.NEAREST_TREE.get(), MemoryStatus.VALUE_PRESENT,
                MemoryModuleTypes.BUSY.get(), MemoryStatus.VALUE_ABSENT
        ), BEHAVIOR_DURATION);
    }

    @Override
    protected boolean checkExtraStartConditions(@NotNull ServerLevel level, @NotNull Villager villager) {
        ItemStack tool = InventoryHelper.getItem(villager, stack -> villager.getProfession()
                .getTool().get().getMatcher().test(stack));
        if (tool.isEmpty()) {
            return false;
        }

        IVillageZone zone = VillagesHelper.getWorkplaceZone(level, villager);
        if (zone == null) {
            return false;
        }

        if (!zone.containsPosition(villager.blockPosition())) {
            return false;
        }

        this.axe = tool;
        this.workplace = zone;
        this.tree = villager.getBrain().getMemory(MemoryModuleTypes.NEAREST_TREE.get())
                .orElseThrow();

        return true;
    }

    @Override
    protected void start(@NotNull ServerLevel level, Villager villager, long gameTime) {
        claimed = workplace.claim(tree.getBase(), villager.getUUID(), BEHAVIOR_DURATION, gameTime);
        if (!claimed) {
            return;
        }

        this.logs =  tree.getBlocks().stream()
                .filter(pos -> level.getBlockState(pos).is(BlockTags.LOGS))
                .toList();

        double normalized = Math.min(logs.size() / 20.0, 1.0);
        double curved = Math.pow(normalized, 0.7);
        this.chopDuration = MINIMUM_DURATION + (int)((BEHAVIOR_DURATION - MINIMUM_DURATION) * curved);

        villager.getBrain().setMemoryWithExpiry(MemoryModuleTypes.BUSY.get(), true, BEHAVIOR_DURATION);
        villager.setItemInHand(InteractionHand.MAIN_HAND, axe);
        VillagerEquipmentSync.sendToNearbyPlayers(villager, InteractionHand.MAIN_HAND, axe);

        if (!villager.blockPosition().closerThan(tree.getBase(), CommonConfig.interactionDistance)) {
            villager.getBrain().setMemoryWithExpiry(MemoryModuleType.WALK_TARGET,
                new WalkTarget(tree.getBase(), 0.5F, 0), BEHAVIOR_DURATION);
        }
    }

    @Override
    protected void stop(@NotNull ServerLevel level, Villager villager, long gameTime) {
        villager.getBrain().eraseMemory(MemoryModuleTypes.BUSY.get());
        villager.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        VillagerEquipmentSync.sendToNearbyPlayers(villager, InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        villager.getBrain().eraseMemory(MemoryModuleTypes.NEAREST_TREE.get());

        if (tree != null) {
            level.destroyBlockProgress(villager.getId(), tree.getBase(), -1);
        }

        this.logs = List.of();
        this.tree = null;
        this.chopTicks = 0;
        this.chopDuration = 0;
        this.axe = ItemStack.EMPTY;
        this.workplace = null;
        this.claimed = false;
    }

    @Override
    protected boolean canStillUse(@NotNull ServerLevel level, @NotNull Villager villager, long gameTime) {
        return claimed;
    }

    @Override
    protected void tick(@NotNull ServerLevel level, @NotNull Villager villager, long gameTime) {
        if (!claimed) {
            return;
        }

        if (!villager.blockPosition().closerThan(tree.getBase(), CommonConfig.interactionDistance)) {
            villager.getBrain().setMemoryWithExpiry(MemoryModuleType.WALK_TARGET,
                new WalkTarget(tree.getBase(), 0.5F, 0), BEHAVIOR_DURATION);
            return;
        }

        villager.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);

        if (chopTicks++ < chopDuration) {
            if (chopTicks % 10 == 0) {
                villager.getLookControl().setLookAt(
                    tree.getBase().getX() + 0.5,
                    tree.getBase().getY() + 0.5,
                    tree.getBase().getZ() + 0.5
                );
                villager.swing(InteractionHand.MAIN_HAND);
                level.playSound(null, tree.getBase(), SoundEvents.WOOD_HIT, SoundSource.BLOCKS, 1.0F, 1.0F);
            }

            int progress = (int)((chopTicks / (float) chopDuration) * 10);
            level.destroyBlockProgress(villager.getId(), tree.getBase(), progress);
            return;
        }

        level.destroyBlockProgress(villager.getId(), tree.getBase(), -1);

        for (BlockPos log : logs) {
            level.destroyBlock(log, true);
            float volume = 0.9F + level.random.nextFloat() * 0.2F;
            float pitch = 0.9F + level.random.nextFloat() * 0.2F;
            level.playSound(null, log, SoundEvents.WOOD_BREAK, SoundSource.BLOCKS, volume, pitch);
        }

        villager.getFoodData().addExhaustion(WORK_EXHAUSTION  * logs.size());
        axe.setDamageValue(axe.getDamageValue() + logs.size());
        if (axe.getDamageValue() >= axe.getMaxDamage()) {
            axe.shrink(1);
            float volume = 0.9F + level.random.nextFloat() * 0.2F;
            float pitch = 0.9F + level.random.nextFloat() * 0.2F;
            level.playSound(null, tree.getBase(), SoundEvents.ITEM_BREAK, SoundSource.NEUTRAL, volume, pitch);
        }

        villager.getBrain().eraseMemory(MemoryModuleTypes.NEAREST_TREE.get());
        claimed = false;
    }
}
