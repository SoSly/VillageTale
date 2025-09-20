package org.sosly.villagetale.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.sosly.villagetale.api.IVillageZone;
import org.sosly.villagetale.entity.MemoryModuleTypes;
import org.sosly.villagetale.entity.Villager;
import org.sosly.villagetale.helper.VillagesHelper;

public class HarvestCropBehavior extends Behavior<Villager> {
    private static final double INTERACTION_DISTANCE = 4.0D;
    private static final int HARVEST_DURATION = 40;
    private static final int BEHAVIOR_DURATION = 100;

    boolean claimed;
    BlockPos pos;
    int harvestTicks;

    public HarvestCropBehavior() {
        super(ImmutableMap.of(
                MemoryModuleTypes.WORK_ZONE.get(), MemoryStatus.VALUE_PRESENT,
                MemoryModuleTypes.NEAREST_HARVESTABLE_CROP.get(), MemoryStatus.VALUE_PRESENT,
                MemoryModuleTypes.BUSY.get(), MemoryStatus.VALUE_ABSENT
        ), BEHAVIOR_DURATION);
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, Villager villager) {
        ItemStack tool = getTool(villager);
        if (tool.isEmpty()) {
            return false;
        }

        IVillageZone zone = getZone(level, villager);
        if (zone == null) {
            return false;
        }

        return zone.containsPosition(villager.blockPosition());
    }

    @Override
    protected void start(ServerLevel level, Villager villager, long gameTime) {
        IVillageZone zone = getZone(level, villager);
        if (zone == null) {
            return;
        }

        pos = villager.getBrain().getMemory(MemoryModuleTypes.NEAREST_HARVESTABLE_CROP.get()).orElse(null);
        if (pos == null) {
            return;
        }

        claimed = zone.claim(pos, villager.getUUID(), BEHAVIOR_DURATION, gameTime);
        if (!claimed) {
            return;
        }

        villager.getBrain().setMemoryWithExpiry(MemoryModuleTypes.BUSY.get(), true, BEHAVIOR_DURATION);
        villager.setItemInHand(InteractionHand.MAIN_HAND, getTool(villager));

        if (villager.blockPosition().closerThan(pos, INTERACTION_DISTANCE)) {
            return;
        }

        villager.getBrain().setMemory(MemoryModuleType.WALK_TARGET,
            new WalkTarget(pos, 0.5F, 1));
    }

    @Override
    protected void stop(ServerLevel level, Villager villager, long gameTime) {
        villager.getBrain().eraseMemory(MemoryModuleTypes.BUSY.get());
        villager.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        pos = null;
        harvestTicks = 0;
    }

    @Override
    protected boolean canStillUse(ServerLevel level, Villager villager, long gameTime) {
        return claimed;
    }

    @Override
    protected void tick(ServerLevel level, Villager villager, long gameTime) {
        if (!claimed || pos == null) {
            return;
        }

        if (!villager.blockPosition().closerThan(pos, INTERACTION_DISTANCE)) {
            return;
        }

        villager.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
        if (harvestTicks++ < HARVEST_DURATION) {
            if (harvestTicks % 10 == 0) {
                villager.getLookControl().setLookAt(
                    pos.getX() + 0.5,
                    pos.getY() + 0.5,
                    pos.getZ() + 0.5
                );
                villager.swing(villager.getUsedItemHand());
            }
            return;
        }

        BlockState cropState = level.getBlockState(pos);
        if (!(cropState.getBlock() instanceof CropBlock cropBlock)) {
            claimed = false;
            return;
        }

        if (cropBlock.getAge(cropState) < cropBlock.getMaxAge()) {
            claimed = false;
            return;
        }

        level.destroyBlock(pos, true);
        level.playSound(null, pos, SoundEvents.CROP_BREAK, SoundSource.BLOCKS, 1.0F, 1.0F);

        ItemStack tool = getTool(villager);
        tool.setDamageValue(tool.getDamageValue() + 1);
        if (tool.getDamageValue() >= tool.getMaxDamage()) {
            tool.shrink(1);
            level.playSound(null, pos, SoundEvents.ITEM_BREAK, SoundSource.NEUTRAL, 1.0F, 1.0F);
        }

        villager.getBrain().eraseMemory(MemoryModuleTypes.NEAREST_HARVESTABLE_CROP.get());
        claimed = false;
    }

    private ItemStack getTool(Villager villager) {
        Container inventory = villager.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            if (inventory.getItem(i).is(ItemTags.HOES)) {
                return inventory.getItem(i);
            }
        }
        return ItemStack.EMPTY;
    }

    private IVillageZone getZone(ServerLevel level, Villager villager) {
        if (villager.getVillage().isEmpty()) {
            return null;
        }

        UUID villageId = villager.getVillage().get();
        UUID workplaceId = villager.getBrain().getMemory(MemoryModuleTypes.WORK_ZONE.get()).orElse(null);
        if (workplaceId == null) {
            return null;
        }

        IVillageZone zone = VillagesHelper.getWorkZone(level, villager, villageId, workplaceId);
        if (zone == null) {
            return null;
        }

        return zone;
    }
}