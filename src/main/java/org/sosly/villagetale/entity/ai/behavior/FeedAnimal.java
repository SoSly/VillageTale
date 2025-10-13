package org.sosly.villagetale.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.sosly.villagetale.api.IVillageZone;
import org.sosly.villagetale.config.CommonConfig;
import org.sosly.villagetale.entity.MemoryModuleTypes;
import org.sosly.villagetale.entity.Villager;
import org.sosly.villagetale.helper.InventoryHelper;
import org.sosly.villagetale.helper.VillagesHelper;
import org.sosly.villagetale.network.packets.clientbound.VillagerEquipmentSync;

import java.util.Optional;
import java.util.UUID;

public class FeedAnimal extends Behavior<Villager> {
    private static final int FEED_DURATION = 40;
    private static final int BEHAVIOR_DURATION = 100;
    private static final float WORK_EXHAUSTION = 0.4f;

    private boolean claimed;
    private Animal targetAnimal;
    private UUID animalId;
    private int feedingTicks;
    private IVillageZone workplace;
    private ItemStack foodItem;

    public FeedAnimal() {
        super(ImmutableMap.of(
                MemoryModuleTypes.WORK_ZONE.get(), MemoryStatus.VALUE_PRESENT,
                MemoryModuleTypes.BREEDABLE_ANIMAL.get(), MemoryStatus.VALUE_PRESENT,
                MemoryModuleTypes.BUSY.get(), MemoryStatus.VALUE_ABSENT
        ), BEHAVIOR_DURATION);
    }

    @Override
    protected boolean checkExtraStartConditions(@NotNull ServerLevel level, @NotNull Villager villager) {
        IVillageZone zone = VillagesHelper.getWorkplaceZone(level, villager);
        if (zone == null) {
            return false;
        }

        if (!zone.containsPosition(villager.blockPosition())) {
            return false;
        }

        Optional<Entity> breedableMemory = villager.getBrain().getMemory(MemoryModuleTypes.BREEDABLE_ANIMAL.get());
        if (breedableMemory.isEmpty() || !(breedableMemory.get() instanceof Animal)) {
            return false;
        }

        Animal animal = (Animal) breedableMemory.get();
        if (!animal.isAlive() || animal.isBaby() || !animal.canFallInLove() || animal.isInLove()) {
            return false;
        }

        ItemStack food = findFoodForAnimal(villager, animal);
        if (food.isEmpty()) {
            return false;
        }

        this.workplace = zone;
        this.foodItem = food;
        return true;
    }

    @Override
    protected void start(@NotNull ServerLevel level, Villager villager, long gameTime) {
        Optional<Entity> breedableMemory = villager.getBrain().getMemory(MemoryModuleTypes.BREEDABLE_ANIMAL.get());
        if (breedableMemory.isEmpty() || !(breedableMemory.get() instanceof Animal)) {
            return;
        }

        Animal animal = (Animal) breedableMemory.get();
        if (!workplace.containsPosition(animal.blockPosition())) {
            return;
        }

        if (!workplace.claim(animal.getUUID(), villager.getUUID(), BEHAVIOR_DURATION, gameTime)) {
            return;
        }

        targetAnimal = animal;
        animalId = animal.getUUID();
        claimed = true;

        if (foodItem.isEmpty()) {
            workplace.release(animalId);
            claimed = false;
            return;
        }

        villager.getBrain().setMemoryWithExpiry(MemoryModuleTypes.BUSY.get(), true, BEHAVIOR_DURATION);
        villager.setItemInHand(InteractionHand.MAIN_HAND, foodItem.copy());
        VillagerEquipmentSync.sendToNearbyPlayers(villager, InteractionHand.MAIN_HAND, foodItem.copy());
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

        if (targetAnimal.isInLove()) {
            return false;
        }

        ItemStack currentFood = findFoodForAnimal(villager, targetAnimal);
        if (currentFood.isEmpty()) {
            return false;
        }

        return feedingTicks < FEED_DURATION;
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

        feedingTicks++;
        if (feedingTicks < FEED_DURATION) {
            if (feedingTicks % 10 == 0) {
                villager.getLookControl().setLookAt(targetAnimal);
                villager.swing(InteractionHand.MAIN_HAND);
            }
            return;
        }

        if (feedingTicks == FEED_DURATION) {
            ItemStack food = findFoodForAnimal(villager, targetAnimal);
            if (food.isEmpty()) {
                return;
            }

            food.shrink(1);
            targetAnimal.setInLove(null);

            level.playSound(null, targetAnimal.blockPosition(), SoundEvents.GENERIC_EAT, SoundSource.NEUTRAL, 0.5F, 1.0F);
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
        feedingTicks = 0;
        foodItem = ItemStack.EMPTY;

        villager.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        VillagerEquipmentSync.sendToNearbyPlayers(villager, InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        villager.getBrain().eraseMemory(MemoryModuleTypes.BUSY.get());
        villager.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
        villager.getBrain().eraseMemory(MemoryModuleTypes.BREEDABLE_ANIMAL.get());
    }

    private ItemStack findFoodForAnimal(Villager villager, Animal animal) {
        return InventoryHelper.getItem(villager, stack -> animal.isFood(stack));
    }
}
