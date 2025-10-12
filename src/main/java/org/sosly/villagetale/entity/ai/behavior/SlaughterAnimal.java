package org.sosly.villagetale.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.sosly.villagetale.api.IVillageZone;
import org.sosly.villagetale.config.CommonConfig;
import org.sosly.villagetale.entity.MemoryModuleTypes;
import org.sosly.villagetale.entity.Villager;
import org.sosly.villagetale.helper.InventoryHelper;
import org.sosly.villagetale.helper.VillagesHelper;
import org.sosly.villagetale.network.packets.clientbound.VillagerEquipmentSync;

public class SlaughterAnimal extends Behavior<Villager> {
    private static final int SLAUGHTER_DURATION = 60;
    private static final int BEHAVIOR_DURATION = 200;
    private static final float WORK_EXHAUSTION = 0.6f;

    private boolean claimed;
    private Animal targetAnimal;
    private UUID animalId;
    private int slaughteringTicks;
    private IVillageZone butchery;

    public SlaughterAnimal() {
        super(ImmutableMap.of(
                MemoryModuleTypes.WORK_ZONE.get(), MemoryStatus.VALUE_PRESENT,
                MemoryModuleTypes.SLAUGHTERABLE_ENTITY.get(), MemoryStatus.VALUE_PRESENT,
                MemoryModuleTypes.BUSY.get(), MemoryStatus.VALUE_ABSENT
        ), BEHAVIOR_DURATION);
    }

    @Override
    protected boolean checkExtraStartConditions(@NotNull ServerLevel level, @NotNull Villager villager) {
        if (InventoryHelper.getItem(villager, stack -> stack.getItem() instanceof AxeItem).isEmpty()) {
            return false;
        }

        IVillageZone zone = VillagesHelper.getWorkplaceZone(level, villager);
        if (zone == null) {
            return false;
        }

        Optional<Entity> slaughterableMemory = villager.getBrain().getMemory(MemoryModuleTypes.SLAUGHTERABLE_ENTITY.get());
        if (slaughterableMemory.isEmpty() || !(slaughterableMemory.get() instanceof Animal)) {
            return false;
        }

        Animal animal = (Animal) slaughterableMemory.get();
        if (!animal.isAlive()) {
            return false;
        }

        if (!zone.containsPosition(animal.blockPosition())) {
            return false;
        }

        this.butchery = zone;
        return true;
    }

    @Override
    protected void start(@NotNull ServerLevel level, Villager villager, long gameTime) {
        Optional<Entity> slaughterableMemory = villager.getBrain().getMemory(MemoryModuleTypes.SLAUGHTERABLE_ENTITY.get());
        if (slaughterableMemory.isEmpty() || !(slaughterableMemory.get() instanceof Animal)) {
            return;
        }

        Animal animal = (Animal) slaughterableMemory.get();
        if (!butchery.containsPosition(animal.blockPosition())) {
            return;
        }

        if (!butchery.claim(animal.getUUID(), villager.getUUID(), BEHAVIOR_DURATION, gameTime)) {
            return;
        }

        targetAnimal = animal;
        animalId = animal.getUUID();
        claimed = true;

        ItemStack axe = InventoryHelper.getItem(villager, stack -> stack.getItem() instanceof AxeItem);
        if (axe.isEmpty()) {
            butchery.release(animalId);
            claimed = false;
            return;
        }

        villager.getBrain().setMemoryWithExpiry(MemoryModuleTypes.BUSY.get(), true, BEHAVIOR_DURATION);
        villager.setItemInHand(InteractionHand.MAIN_HAND, axe);
        VillagerEquipmentSync.sendToNearbyPlayers(villager, InteractionHand.MAIN_HAND, axe);
        villager.getBrain().setMemory(MemoryModuleType.WALK_TARGET,
                new WalkTarget(targetAnimal.blockPosition(), 0.5f, 2));
    }

    @Override
    protected boolean canStillUse(@NotNull ServerLevel level, @NotNull Villager villager, long gameTime) {
        if (!claimed || targetAnimal == null || !targetAnimal.isAlive()) {
            return false;
        }

        if (!butchery.containsPosition(villager.blockPosition())) {
            return false;
        }

        if (InventoryHelper.getItem(villager, stack -> stack.getItem() instanceof AxeItem).isEmpty()) {
            return false;
        }

        return true;
    }

    @Override
    protected void tick(@NotNull ServerLevel level, @NotNull Villager villager, long gameTime) {
        if (!claimed || targetAnimal == null || !targetAnimal.isAlive()) {
            return;
        }

        if (villager.distanceToSqr(targetAnimal) > CommonConfig.interactionDistance * CommonConfig.interactionDistance) {
            villager.getBrain().setMemory(MemoryModuleType.WALK_TARGET,
                    new WalkTarget(targetAnimal.blockPosition(), 0.5f, 2));
            return;
        }

        villager.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);

        slaughteringTicks++;
        if (slaughteringTicks < SLAUGHTER_DURATION) {
            if (slaughteringTicks % 10 == 0) {
                villager.getLookControl().setLookAt(targetAnimal);
                villager.swing(InteractionHand.MAIN_HAND);
            }
            return;
        }

        if (slaughteringTicks == SLAUGHTER_DURATION) {
            ItemStack axe = InventoryHelper.getItem(villager, stack -> stack.getItem() instanceof AxeItem);
            if (axe.isEmpty()) {
                return;
            }

            DamageSource damageSource = level.damageSources().mobAttack(villager);
            targetAnimal.hurt(damageSource, Float.MAX_VALUE);

            axe.hurtAndBreak(1, villager, v -> v.broadcastBreakEvent(InteractionHand.MAIN_HAND));

            villager.getFoodData().addExhaustion(WORK_EXHAUSTION);
        }
    }

    @Override
    protected void stop(@NotNull ServerLevel level, @NotNull Villager villager, long gameTime) {
        if (claimed && animalId != null) {
            butchery.release(animalId);
        }

        claimed = false;
        targetAnimal = null;
        animalId = null;
        slaughteringTicks = 0;

        villager.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        VillagerEquipmentSync.sendToNearbyPlayers(villager, InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        villager.getBrain().eraseMemory(MemoryModuleTypes.BUSY.get());
        villager.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
        villager.getBrain().eraseMemory(MemoryModuleTypes.SLAUGHTERABLE_ENTITY.get());
    }
}
