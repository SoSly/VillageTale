package org.sosly.villagetale.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.Container;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.sosly.villagetale.api.IVillageZone;
import org.sosly.villagetale.entity.MemoryModuleTypes;
import org.sosly.villagetale.entity.Villager;
import org.sosly.villagetale.helper.VillagesHelper;

public class PlantSeeds extends Behavior<Villager> {
    private static final double INTERACTION_DISTANCE = 4.0D;
    private static final int PLANTING_DURATION = 30;
    private static final int BEHAVIOR_DURATION = 60;

    boolean claimed;
    BlockPos pos;
    int plantTicks;

    public PlantSeeds() {
        super(ImmutableMap.of(
                MemoryModuleTypes.WORK_ZONE.get(), MemoryStatus.VALUE_PRESENT,
                MemoryModuleTypes.NEAREST_EMPTY_FARMLAND.get(), MemoryStatus.VALUE_PRESENT,
                MemoryModuleTypes.BUSY.get(), MemoryStatus.VALUE_ABSENT
        ), BEHAVIOR_DURATION);
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, Villager villager) {
        if (getSeeds(villager).isEmpty()) {
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

        pos = villager.getBrain().getMemory(MemoryModuleTypes.NEAREST_EMPTY_FARMLAND.get()).orElse(null);
        if (pos == null) {
            return;
        }

        claimed = zone.claim(pos, villager.getUUID(), BEHAVIOR_DURATION, gameTime);
        if (!claimed) {
            return;
        }

        villager.getBrain().setMemoryWithExpiry(MemoryModuleTypes.BUSY.get(), true, BEHAVIOR_DURATION);

        if (villager.blockPosition().closerThan(pos, INTERACTION_DISTANCE)) {
            return;
        }

        villager.getBrain().setMemory(MemoryModuleType.WALK_TARGET,
            new WalkTarget(pos, 0.5F, 1));
    }

    @Override
    protected void stop(ServerLevel level, Villager villager, long gameTime) {
        villager.getBrain().eraseMemory(MemoryModuleTypes.BUSY.get());
        pos = null;
        plantTicks = 0;
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
        if (plantTicks++ < PLANTING_DURATION) {
            if (plantTicks % 10 == 0) {
                villager.getLookControl().setLookAt(
                    pos.getX() + 0.5,
                    pos.getY() + 0.5,
                    pos.getZ() + 0.5
                );
                villager.swing(villager.getUsedItemHand());
            }
            return;
        }

        ItemStack seeds = getSeeds(villager);
        if (seeds.isEmpty()) {
            claimed = false;
            return;
        }

        BlockState cropBlock = getCropBlockForSeed(seeds);
        if (cropBlock != null) {
            level.setBlock(pos, cropBlock, 3);
            level.playSound(null, pos, SoundEvents.CROP_PLANTED, SoundSource.BLOCKS, 1.0F, 1.0F);
            seeds.shrink(1);
        }

        villager.getBrain().eraseMemory(MemoryModuleTypes.NEAREST_EMPTY_FARMLAND.get());
        claimed = false;
    }

    private ItemStack getSeeds(Villager villager) {
        Container inventory = villager.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.is(ItemTags.VILLAGER_PLANTABLE_SEEDS)) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    private BlockState getCropBlockForSeed(ItemStack seed) {
        if (seed.is(Items.WHEAT_SEEDS)) {
            return Blocks.WHEAT.defaultBlockState();
        } else if (seed.is(Items.BEETROOT_SEEDS)) {
            return Blocks.BEETROOTS.defaultBlockState();
        } else if (seed.is(Items.CARROT)) {
            return Blocks.CARROTS.defaultBlockState();
        } else if (seed.is(Items.POTATO)) {
            return Blocks.POTATOES.defaultBlockState();
        }
        return null;
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