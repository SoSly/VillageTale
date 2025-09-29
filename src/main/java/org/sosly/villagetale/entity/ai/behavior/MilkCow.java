package org.sosly.villagetale.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;
import org.sosly.villagetale.api.IVillageZone;
import org.sosly.villagetale.entity.MemoryModuleTypes;
import org.sosly.villagetale.entity.Villager;
import org.sosly.villagetale.helper.InventoryHelper;
import org.sosly.villagetale.helper.VillagesHelper;

import java.util.List;
import java.util.UUID;

public class MilkCow extends Behavior<Villager> {
    private static final int MILK_DURATION = 40;
    private static final int BEHAVIOR_DURATION = 100;
    private static final float WORK_EXHAUSTION = 0.4f;

    private boolean claimed;
    private Cow targetCow;
    private UUID cowId;
    private int milkingTicks;
    private ItemStack bucket;
    private IVillageZone workplace;

    public MilkCow() {
        super(ImmutableMap.of(
                MemoryModuleTypes.WORK_ZONE.get(), MemoryStatus.VALUE_PRESENT,
                MemoryModuleTypes.BUSY.get(), MemoryStatus.VALUE_ABSENT
        ), BEHAVIOR_DURATION);
    }

    @Override
    protected boolean checkExtraStartConditions(@NotNull ServerLevel level, @NotNull Villager villager) {
        ItemStack bucket = InventoryHelper.getItem(villager, stack -> stack.is(Items.BUCKET));
        if (bucket.isEmpty()) {
            return false;
        }

        IVillageZone zone = VillagesHelper.getWorkplaceZone(level, villager);
        if (zone == null) {
            return false;
        }

        if (!zone.containsPosition(villager.blockPosition())) {
            return false;
        }

        List<Cow> cows = level.getEntitiesOfClass(Cow.class, villager.getBoundingBox().inflate(10.0));
        if (cows.isEmpty()) {
            return false;
        }

        this.bucket = bucket;
        this.workplace = zone;
        return true;
    }

    @Override
    protected void start(@NotNull ServerLevel level, Villager villager, long gameTime) {
        List<Cow> cows = level.getEntitiesOfClass(Cow.class, villager.getBoundingBox().inflate(10.0));

        for (Cow cow : cows) {
            if (!workplace.containsPosition(cow.blockPosition())) {
                continue;
            }

            if (workplace.claim(cow.getUUID(), villager.getUUID(), BEHAVIOR_DURATION, gameTime)) {
                targetCow = cow;
                cowId = cow.getUUID();
                claimed = true;
                break;
            }
        }

        if (!claimed) {
            return;
        }

        villager.getBrain().setMemoryWithExpiry(MemoryModuleTypes.BUSY.get(), true, BEHAVIOR_DURATION);
        villager.setItemInHand(InteractionHand.MAIN_HAND, bucket);
        villager.getBrain().setMemory(MemoryModuleType.WALK_TARGET,
            new WalkTarget(targetCow.blockPosition(), 0.5f, 2));
    }

    @Override
    protected boolean canStillUse(@NotNull ServerLevel level, @NotNull Villager villager, long gameTime) {
        if (!claimed || targetCow == null || !targetCow.isAlive()) {
            return false;
        }

        if (!workplace.containsPosition(villager.blockPosition())) {
            return false;
        }

        return milkingTicks < MILK_DURATION;
    }

    @Override
    protected void tick(@NotNull ServerLevel level, @NotNull Villager villager, long gameTime) {
        if (!claimed || targetCow == null) {
            return;
        }

        if (villager.distanceToSqr(targetCow) > 4.0) {
            villager.getBrain().setMemory(MemoryModuleType.WALK_TARGET,
                new WalkTarget(targetCow.blockPosition(), 0.5f, 2));
            return;
        }

        villager.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
        villager.getLookControl().setLookAt(targetCow);

        if (milkingTicks++ >= MILK_DURATION) {
            bucket.shrink(1);
            ItemStack milkBucket = new ItemStack(Items.MILK_BUCKET);
//            if (villager.getInventory().addItem(milkBucket)) {
//                villager.spawnAtLocation(milkBucket);
//            }
            villager.getFoodData().addExhaustion(WORK_EXHAUSTION);
        }
    }

    @Override
    protected void stop(@NotNull ServerLevel level, @NotNull Villager villager, long gameTime) {
        if (claimed && cowId != null) {
            workplace.release(cowId);
        }

        claimed = false;
        targetCow = null;
        cowId = null;
        milkingTicks = 0;

        villager.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        villager.getBrain().eraseMemory(MemoryModuleTypes.BUSY.get());
        villager.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
    }
}
