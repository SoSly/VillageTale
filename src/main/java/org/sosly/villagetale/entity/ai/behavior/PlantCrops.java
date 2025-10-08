package org.sosly.villagetale.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.sosly.villagetale.api.IVillageZone;
import org.sosly.villagetale.config.CommonConfig;
import org.sosly.villagetale.entity.MemoryModuleTypes;
import org.sosly.villagetale.entity.Villager;
import org.sosly.villagetale.helper.InventoryHelper;
import org.sosly.villagetale.helper.VillagesHelper;
import org.sosly.villagetale.network.packets.clientbound.VillagerEquipmentSync;

public class PlantCrops extends Behavior<Villager> {
    private static final int PLANTING_DURATION = 30;
    private static final int BEHAVIOR_DURATION = 60;
    private static final float WORK_EXHAUSTION = 0.4f;

    boolean claimed;
    BlockPos pos;
    int plantTicks;

    ItemStack seeds = ItemStack.EMPTY;
    IVillageZone workplace;

    public PlantCrops() {
        super(ImmutableMap.of(
                MemoryModuleTypes.WORK_ZONE.get(), MemoryStatus.VALUE_PRESENT,
                MemoryModuleTypes.NEAREST_EMPTY_FARMLAND.get(), MemoryStatus.VALUE_PRESENT,
                MemoryModuleTypes.BUSY.get(), MemoryStatus.VALUE_ABSENT
        ), BEHAVIOR_DURATION);
    }

    @Override
    protected boolean checkExtraStartConditions(@NotNull ServerLevel level, @NotNull Villager villager) {
        IVillageZone zone = VillagesHelper.getWorkplaceZone(level, villager);
        if (zone == null) {
            return false;
        }

        ItemStack seeds = InventoryHelper.getItem(villager, stack -> stack.is(ItemTags.VILLAGER_PLANTABLE_SEEDS), zone);
        if (seeds.isEmpty()) {
            return false;
        }

        if (!zone.containsPosition(villager.blockPosition())) {
            return false;
        }

        this.seeds = seeds;
        this.workplace = zone;
        return true;
    }

    @Override
    protected void start(@NotNull ServerLevel level, Villager villager, long gameTime) {
        pos = villager.getBrain().getMemory(MemoryModuleTypes.NEAREST_EMPTY_FARMLAND.get()).orElse(null);
        if (pos == null) {
            return;
        }

        claimed = workplace.claim(pos, villager.getUUID(), BEHAVIOR_DURATION, gameTime);
        if (!claimed) {
            return;
        }

        villager.getBrain().setMemoryWithExpiry(MemoryModuleTypes.BUSY.get(), true, BEHAVIOR_DURATION);
        villager.setItemInHand(InteractionHand.MAIN_HAND, seeds);
        VillagerEquipmentSync.sendToNearbyPlayers(villager, InteractionHand.MAIN_HAND, seeds);

        if (villager.blockPosition().closerThan(pos, CommonConfig.interactionDistance)) {
            return;
        }

        villager.getBrain().setMemoryWithExpiry(MemoryModuleType.WALK_TARGET,
            new WalkTarget(pos, 0.5F, 1), 200L);
    }

    @Override
    protected void stop(@NotNull ServerLevel level, Villager villager, long gameTime) {
        villager.getBrain().eraseMemory(MemoryModuleTypes.BUSY.get());
        villager.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        VillagerEquipmentSync.sendToNearbyPlayers(villager, InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        villager.getBrain().eraseMemory(MemoryModuleTypes.NEAREST_EMPTY_FARMLAND.get());

        this.pos = null;
        this.plantTicks = 0;
        this.seeds = ItemStack.EMPTY;
        this.workplace = null;
    }

    @Override
    protected boolean canStillUse(@NotNull ServerLevel level, @NotNull Villager villager, long gameTime) {
        return claimed;
    }

    @Override
    protected void tick(@NotNull ServerLevel level, @NotNull Villager villager, long gameTime) {
        if (!claimed || pos == null) {
            return;
        }

        if (!villager.blockPosition().closerThan(pos, CommonConfig.interactionDistance)) {
            return;
        }

        villager.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
        if (plantTicks++ < PLANTING_DURATION) {
            if (plantTicks % 10 == 0) {
                villager.getLookControl().setLookAt(
                    pos.getX() + 0.5,
                    pos.getY() + 0.5,
                    pos.getZ() + 0.5
                );
                villager.swing(InteractionHand.MAIN_HAND);
            }
            return;
        }

        BlockState cropBlock = getCropBlock(seeds);
        if (cropBlock != null) {
            level.setBlock(pos.above(), cropBlock, 3);
            level.playSound(null, pos, SoundEvents.CROP_PLANTED, SoundSource.BLOCKS, 1.0F, 1.0F);
            villager.getFoodData().addExhaustion(WORK_EXHAUSTION);
            seeds.shrink(1);
        }

        villager.getBrain().eraseMemory(MemoryModuleTypes.NEAREST_EMPTY_FARMLAND.get());
        claimed = false;
    }

    private BlockState getCropBlock(ItemStack crop) {
        if (crop.getItem() instanceof BlockItem blockItem) {
            Block block = blockItem.getBlock();
            return block.defaultBlockState();
        }
        return null;
    }
}
