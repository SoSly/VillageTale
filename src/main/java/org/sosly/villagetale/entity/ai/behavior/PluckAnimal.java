package org.sosly.villagetale.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;
import net.minecraft.nbt.CompoundTag;
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
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import org.jetbrains.annotations.NotNull;
import org.sosly.villagetale.api.IVillageZone;
import org.sosly.villagetale.config.CommonConfig;
import org.sosly.villagetale.data.NBTTags;
import org.sosly.villagetale.entity.MemoryModuleTypes;
import org.sosly.villagetale.entity.Villager;
import org.sosly.villagetale.helper.VillagesHelper;

public class PluckAnimal extends Behavior<Villager> {
    private static final int PLUCK_DURATION = 40;
    private static final int BEHAVIOR_DURATION = 100;
    private static final float WORK_EXHAUSTION = 0.4f;
    private static final Random RANDOM = new Random();

    private boolean claimed;
    private Animal targetAnimal;
    private UUID animalId;
    private int pluckingTicks;
    private IVillageZone workplace;

    public PluckAnimal() {
        super(ImmutableMap.of(
                MemoryModuleTypes.WORK_ZONE.get(), MemoryStatus.VALUE_PRESENT,
                MemoryModuleTypes.PLUCKABLE_ANIMAL.get(), MemoryStatus.VALUE_PRESENT,
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

        Optional<Entity> pluckableMemory = villager.getBrain().getMemory(MemoryModuleTypes.PLUCKABLE_ANIMAL.get());
        if (pluckableMemory.isEmpty() || !(pluckableMemory.get() instanceof Animal)) {
            return false;
        }

        Animal animal = (Animal) pluckableMemory.get();
        if (!animal.isAlive() || animal.isBaby()) {
            return false;
        }

        if (!isPluckable(animal, level.getGameTime())) {
            return false;
        }

        this.workplace = zone;
        return true;
    }

    @Override
    protected void start(@NotNull ServerLevel level, Villager villager, long gameTime) {
        Optional<Entity> pluckableMemory = villager.getBrain().getMemory(MemoryModuleTypes.PLUCKABLE_ANIMAL.get());
        if (pluckableMemory.isEmpty() || !(pluckableMemory.get() instanceof Animal)) {
            return;
        }

        Animal animal = (Animal) pluckableMemory.get();
        if (!workplace.containsPosition(animal.blockPosition())) {
            return;
        }

        if (!workplace.claim(animal.getUUID(), villager.getUUID(), BEHAVIOR_DURATION, gameTime)) {
            return;
        }

        targetAnimal = animal;
        animalId = animal.getUUID();
        claimed = true;

        villager.getBrain().setMemoryWithExpiry(MemoryModuleTypes.BUSY.get(), true, BEHAVIOR_DURATION);
        villager.swing(InteractionHand.MAIN_HAND);
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

        return pluckingTicks < PLUCK_DURATION;
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

        pluckingTicks++;
        if (pluckingTicks < PLUCK_DURATION) {
            if (pluckingTicks % 10 == 0) {
                villager.getLookControl().setLookAt(targetAnimal);
                villager.swing(InteractionHand.MAIN_HAND);
            }
            return;
        }

        if (pluckingTicks == PLUCK_DURATION) {
            List<ItemStack> feathers = getFeathersFromLootTable(level, targetAnimal);

            if (feathers.isEmpty()) {
                int featherCount = 1 + RANDOM.nextInt(3);
                feathers.add(new ItemStack(Items.FEATHER, featherCount));
            }

            for (ItemStack featherStack : feathers) {
                if (!villager.getInventory().canAddItem(featherStack)) {
                    villager.spawnAtLocation(featherStack);
                } else {
                    villager.getInventory().addItem(featherStack);
                }
            }

            CompoundTag tag = targetAnimal.getPersistentData();
            tag.putLong(NBTTags.PLUCK_COOLDOWN, gameTime);

            level.playSound(null, targetAnimal.blockPosition(), SoundEvents.CHICKEN_HURT, SoundSource.NEUTRAL, 0.8F, 1.0F);
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
        pluckingTicks = 0;

        villager.getBrain().eraseMemory(MemoryModuleTypes.BUSY.get());
        villager.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
        villager.getBrain().eraseMemory(MemoryModuleTypes.PLUCKABLE_ANIMAL.get());
    }

    private boolean isPluckable(Animal animal, long currentTime) {
        CompoundTag tag = animal.getPersistentData();
        if (!tag.contains(NBTTags.PLUCK_COOLDOWN)) {
            return true;
        }

        long lastPluckTime = tag.getLong(NBTTags.PLUCK_COOLDOWN);
        return currentTime - lastPluckTime >= CommonConfig.pluckCooldownTicks;
    }

    private List<ItemStack> getFeathersFromLootTable(ServerLevel level, Animal animal) {
        try {
            LootTable lootTable = level.getServer().getLootData()
                .getLootTable(animal.getLootTable());

            LootParams.Builder paramsBuilder = new LootParams.Builder(level);
            paramsBuilder.withParameter(LootContextParams.ORIGIN, animal.position());
            paramsBuilder.withParameter(LootContextParams.THIS_ENTITY, animal);
            paramsBuilder.withParameter(LootContextParams.DAMAGE_SOURCE, level.damageSources().generic());

            LootParams lootParams = paramsBuilder.create(LootContextParamSets.ENTITY);
            List<ItemStack> loot = lootTable.getRandomItems(lootParams);

            return loot.stream()
                .filter(stack -> stack.is(Items.FEATHER))
                .collect(Collectors.toList());
        } catch (Exception e) {
            return List.of();
        }
    }
}
