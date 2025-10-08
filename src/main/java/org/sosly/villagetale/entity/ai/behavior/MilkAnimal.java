package org.sosly.villagetale.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.MushroomCow;
import net.minecraft.world.entity.animal.goat.Goat;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;
import org.sosly.villagetale.api.IVillageZone;
import org.sosly.villagetale.config.CommonConfig;
import org.sosly.villagetale.data.NBTTags;
import org.sosly.villagetale.entity.MemoryModuleTypes;
import org.sosly.villagetale.entity.Villager;
import org.sosly.villagetale.helper.InventoryHelper;
import org.sosly.villagetale.helper.VillagesHelper;

import java.util.Optional;
import org.sosly.villagetale.network.packets.clientbound.VillagerEquipmentSync;

import java.util.UUID;

public class MilkAnimal extends Behavior<Villager> {
    private static final int MILK_DURATION = 40;
    private static final int BEHAVIOR_DURATION = 100;
    private static final float WORK_EXHAUSTION = 0.4f;

    private boolean claimed;
    private Animal targetAnimal;
    private UUID animalId;
    private int milkingTicks;
    private IVillageZone workplace;

    public MilkAnimal() {
        super(ImmutableMap.of(
                MemoryModuleTypes.WORK_ZONE.get(), MemoryStatus.VALUE_PRESENT,
                MemoryModuleTypes.MILKABLE_ANIMAL.get(), MemoryStatus.VALUE_PRESENT,
                MemoryModuleTypes.BUSY.get(), MemoryStatus.VALUE_ABSENT
        ), BEHAVIOR_DURATION);
    }

    @Override
    protected boolean checkExtraStartConditions(@NotNull ServerLevel level, @NotNull Villager villager) {
        if (InventoryHelper.getItem(villager, stack -> stack.is(Items.BUCKET)).isEmpty()) {
            return false;
        }

        IVillageZone zone = VillagesHelper.getWorkplaceZone(level, villager);
        if (zone == null) {
            return false;
        }

        if (!zone.containsPosition(villager.blockPosition())) {
            return false;
        }

        Optional<Entity> milkableMemory = villager.getBrain().getMemory(MemoryModuleTypes.MILKABLE_ANIMAL.get());
        if (milkableMemory.isEmpty() || !(milkableMemory.get() instanceof Animal)) {
            return false;
        }

        Animal animal = (Animal) milkableMemory.get();
        if (!animal.isAlive() || animal.isBaby()) {
            return false;
        }

        if (!(animal instanceof Cow || animal instanceof MushroomCow || animal instanceof Goat)) {
            return false;
        }

        if (!isMilkable(animal, level.getGameTime())) {
            return false;
        }

        this.workplace = zone;
        return true;
    }

    @Override
    protected void start(@NotNull ServerLevel level, Villager villager, long gameTime) {
        Optional<Entity> milkableMemory = villager.getBrain().getMemory(MemoryModuleTypes.MILKABLE_ANIMAL.get());
        if (milkableMemory.isEmpty() || !(milkableMemory.get() instanceof Animal)) {
            return;
        }

        Animal animal = (Animal) milkableMemory.get();
        if (!workplace.containsPosition(animal.blockPosition())) {
            return;
        }

        if (!workplace.claim(animal.getUUID(), villager.getUUID(), BEHAVIOR_DURATION, gameTime)) {
            return;
        }

        targetAnimal = animal;
        animalId = animal.getUUID();
        claimed = true;

        ItemStack bucket = InventoryHelper.getItem(villager, stack -> stack.is(Items.BUCKET));
        if (bucket.isEmpty()) {
            workplace.release(animalId);
            claimed = false;
            return;
        }

        villager.getBrain().setMemoryWithExpiry(MemoryModuleTypes.BUSY.get(), true, BEHAVIOR_DURATION);
        villager.setItemInHand(InteractionHand.MAIN_HAND, bucket);
        VillagerEquipmentSync.sendToNearbyPlayers(villager, InteractionHand.MAIN_HAND, bucket);
        villager.getBrain().setMemory(MemoryModuleType.WALK_TARGET,
            new WalkTarget(targetAnimal.blockPosition(), 0.5f, 2));
    }

    @Override
    protected boolean canStillUse(@NotNull ServerLevel level, @NotNull Villager villager, long gameTime) {
        if (!claimed || targetAnimal == null || !targetAnimal.isAlive()) {
            return false;
        }

        if (!workplace.containsPosition(villager.blockPosition())) {
            return false;
        }

        if (InventoryHelper.getItem(villager, stack -> stack.is(Items.BUCKET)).isEmpty()) {
            return false;
        }

        return milkingTicks < MILK_DURATION;
    }

    @Override
    protected void tick(@NotNull ServerLevel level, @NotNull Villager villager, long gameTime) {
        if (!claimed || targetAnimal == null) {
            return;
        }

        if (villager.distanceToSqr(targetAnimal) > CommonConfig.interactionDistance * CommonConfig.interactionDistance) {
            villager.getBrain().setMemory(MemoryModuleType.WALK_TARGET,
                new WalkTarget(targetAnimal.blockPosition(), 0.5f, 2));
            return;
        }

        villager.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);

        milkingTicks++;
        if (milkingTicks < MILK_DURATION) {
            if (milkingTicks % 10 == 0) {
                villager.getLookControl().setLookAt(targetAnimal);
                villager.swing(InteractionHand.MAIN_HAND);
            }
            return;
        }

        if (milkingTicks == MILK_DURATION) {
            ItemStack bucket = InventoryHelper.getItem(villager, stack -> stack.is(Items.BUCKET));
            if (bucket.isEmpty()) {
                return;
            }

            bucket.shrink(1);
            ItemStack milkBucket = new ItemStack(Items.MILK_BUCKET);
            if (!villager.getInventory().canAddItem(milkBucket)) {
                villager.spawnAtLocation(milkBucket);
            } else {
                villager.getInventory().addItem(milkBucket);
            }

            CompoundTag tag = targetAnimal.getPersistentData();
            tag.putLong(NBTTags.MILK_COOLDOWN, gameTime);

            level.playSound(null, targetAnimal.blockPosition(), getMilkSound(), SoundSource.NEUTRAL, 1.0F, 1.0F);
            villager.getFoodData().addExhaustion(WORK_EXHAUSTION);
        }
    }

    @Override
    protected void stop(@NotNull ServerLevel level, @NotNull Villager villager, long gameTime) {
        if (claimed && animalId != null) {
            workplace.release(animalId);
        }

        claimed = false;
        targetAnimal = null;
        animalId = null;
        milkingTicks = 0;

        villager.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        VillagerEquipmentSync.sendToNearbyPlayers(villager, InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        villager.getBrain().eraseMemory(MemoryModuleTypes.BUSY.get());
        villager.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
        villager.getBrain().eraseMemory(MemoryModuleTypes.MILKABLE_ANIMAL.get());
    }

    private boolean isMilkable(Animal animal, long currentTime) {
        CompoundTag tag = animal.getPersistentData();
        if (!tag.contains(NBTTags.MILK_COOLDOWN)) {
            return true;
        }

        long lastMilkTime = tag.getLong(NBTTags.MILK_COOLDOWN);
        return currentTime - lastMilkTime >= CommonConfig.milkCooldownTicks;
    }

    private SoundEvent getMilkSound() {
        if (targetAnimal instanceof Goat) {
            return SoundEvents.GOAT_SCREAMING_MILK;
        }
        return SoundEvents.COW_MILK;
    }
}
