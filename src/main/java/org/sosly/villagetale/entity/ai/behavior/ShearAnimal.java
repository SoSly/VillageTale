package org.sosly.villagetale.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShearsItem;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import org.jetbrains.annotations.NotNull;
import org.sosly.villagetale.api.IVillageZone;
import org.sosly.villagetale.config.CommonConfig;
import org.sosly.villagetale.entity.MemoryModuleTypes;
import org.sosly.villagetale.entity.Villager;
import org.sosly.villagetale.helper.InventoryHelper;
import org.sosly.villagetale.helper.VillagesHelper;
import org.sosly.villagetale.network.NetworkHandler;

public class ShearAnimal extends Behavior<Villager> {
    private static final int SHEAR_DURATION = 40;
    private static final int BEHAVIOR_DURATION = 100;
    private static final float WORK_EXHAUSTION = 0.4f;
    private static final Random RANDOM = new Random();

    private boolean claimed;
    private Sheep targetSheep;
    private UUID sheepId;
    private int shearingTicks;
    private IVillageZone workplace;

    public ShearAnimal() {
        super(ImmutableMap.of(
                MemoryModuleTypes.WORK_ZONE.get(), MemoryStatus.VALUE_PRESENT,
                MemoryModuleTypes.SHEARABLE_ANIMAL.get(), MemoryStatus.VALUE_PRESENT,
                MemoryModuleTypes.BUSY.get(), MemoryStatus.VALUE_ABSENT
        ), BEHAVIOR_DURATION);
    }

    @Override
    protected boolean checkExtraStartConditions(@NotNull ServerLevel level, @NotNull Villager villager) {
        if (InventoryHelper.getItem(villager, stack -> stack.getItem() instanceof ShearsItem).isEmpty()) {
            return false;
        }

        IVillageZone zone = VillagesHelper.getWorkplaceZone(level, villager);
        if (zone == null) {
            return false;
        }

        if (!zone.containsPosition(villager.blockPosition())) {
            return false;
        }

        Optional<Entity> shearableMemory = villager.getBrain().getMemory(MemoryModuleTypes.SHEARABLE_ANIMAL.get());
        if (shearableMemory.isEmpty() || !(shearableMemory.get() instanceof Sheep)) {
            return false;
        }

        Sheep sheep = (Sheep) shearableMemory.get();
        if (!sheep.isAlive() || sheep.isBaby() || sheep.isSheared()) {
            return false;
        }

        this.workplace = zone;
        return true;
    }

    @Override
    protected void start(@NotNull ServerLevel level, Villager villager, long gameTime) {
        Optional<Entity> shearableMemory = villager.getBrain().getMemory(MemoryModuleTypes.SHEARABLE_ANIMAL.get());
        if (shearableMemory.isEmpty() || !(shearableMemory.get() instanceof Sheep)) {
            return;
        }

        Sheep sheep = (Sheep) shearableMemory.get();
        if (!workplace.containsPosition(sheep.blockPosition())) {
            return;
        }

        if (!workplace.claim(sheep.getUUID(), villager.getUUID(), BEHAVIOR_DURATION, gameTime)) {
            return;
        }

        targetSheep = sheep;
        sheepId = sheep.getUUID();
        claimed = true;

        ItemStack shears = InventoryHelper.getItem(villager, stack -> stack.getItem() instanceof ShearsItem);
        if (shears.isEmpty()) {
            workplace.release(sheepId);
            claimed = false;
            return;
        }

        villager.getBrain().setMemoryWithExpiry(MemoryModuleTypes.BUSY.get(), true, BEHAVIOR_DURATION);
        villager.setItemInHand(InteractionHand.MAIN_HAND, shears);
        NetworkHandler.syncEquipmentToNearbyPlayers(villager, InteractionHand.MAIN_HAND, shears);
        villager.getBrain().setMemory(MemoryModuleType.WALK_TARGET,
            new WalkTarget(targetSheep.blockPosition(), 0.5f, 2));
    }

    @Override
    protected boolean canStillUse(@NotNull ServerLevel level, @NotNull Villager villager, long gameTime) {
        if (!claimed || targetSheep == null || !targetSheep.isAlive()) {
            return false;
        }

        if (!workplace.containsPosition(villager.blockPosition())) {
            return false;
        }

        if (InventoryHelper.getItem(villager, stack -> stack.getItem() instanceof ShearsItem).isEmpty()) {
            return false;
        }

        return shearingTicks < SHEAR_DURATION;
    }

    @Override
    protected void tick(@NotNull ServerLevel level, @NotNull Villager villager, long gameTime) {
        if (!claimed || targetSheep == null) {
            return;
        }

        if (villager.distanceToSqr(targetSheep) > CommonConfig.interactionDistance * CommonConfig.interactionDistance) {
            villager.getBrain().setMemory(MemoryModuleType.WALK_TARGET,
                new WalkTarget(targetSheep.blockPosition(), 0.5f, 2));
            return;
        }

        villager.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);

        shearingTicks++;
        if (shearingTicks < SHEAR_DURATION) {
            if (shearingTicks % 10 == 0) {
                villager.getLookControl().setLookAt(targetSheep);
                villager.swing(InteractionHand.MAIN_HAND);
            }
            return;
        }

        if (shearingTicks == SHEAR_DURATION) {
            ItemStack shears = InventoryHelper.getItem(villager, stack -> stack.getItem() instanceof ShearsItem);
            if (shears.isEmpty()) {
                return;
            }

            List<ItemStack> wool = getWoolFromLootTable(level, targetSheep);

            if (wool.isEmpty()) {
                int woolCount = 1 + RANDOM.nextInt(3);
                ItemStack woolStack = new ItemStack(getWoolItem(targetSheep.getColor()), woolCount);
                wool.add(woolStack);
            }

            for (ItemStack woolStack : wool) {
                if (!villager.getInventory().canAddItem(woolStack)) {
                    villager.spawnAtLocation(woolStack);
                } else {
                    villager.getInventory().addItem(woolStack);
                }
            }

            targetSheep.setSheared(true);

            shears.hurtAndBreak(1, villager, v -> v.broadcastBreakEvent(InteractionHand.MAIN_HAND));

            level.playSound(null, targetSheep.blockPosition(), SoundEvents.SHEEP_SHEAR, SoundSource.NEUTRAL, 1.0F, 1.0F);
            villager.getFoodData().addExhaustion(WORK_EXHAUSTION);
        }
    }

    @Override
    protected void stop(@NotNull ServerLevel level, @NotNull Villager villager, long gameTime) {
        if (claimed && sheepId != null) {
            workplace.release(sheepId);
        }

        claimed = false;
        targetSheep = null;
        sheepId = null;
        shearingTicks = 0;

        villager.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        NetworkHandler.syncEquipmentToNearbyPlayers(villager, InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        villager.getBrain().eraseMemory(MemoryModuleTypes.BUSY.get());
        villager.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
        villager.getBrain().eraseMemory(MemoryModuleTypes.SHEARABLE_ANIMAL.get());
    }

    private List<ItemStack> getWoolFromLootTable(ServerLevel level, Sheep sheep) {
        try {
            LootTable lootTable = level.getServer().getLootData()
                .getLootTable(sheep.getLootTable());

            LootParams.Builder paramsBuilder = new LootParams.Builder(level);
            paramsBuilder.withParameter(LootContextParams.ORIGIN, sheep.position());
            paramsBuilder.withParameter(LootContextParams.THIS_ENTITY, sheep);
            paramsBuilder.withParameter(LootContextParams.DAMAGE_SOURCE, level.damageSources().generic());

            LootParams lootParams = paramsBuilder.create(LootContextParamSets.ENTITY);
            List<ItemStack> loot = lootTable.getRandomItems(lootParams);

            return loot.stream()
                .filter(stack -> stack.getItem().toString().contains("wool"))
                .collect(Collectors.toList());
        } catch (Exception e) {
            return List.of();
        }
    }

    private Item getWoolItem(DyeColor color) {
        return switch (color) {
            case ORANGE -> Blocks.ORANGE_WOOL.asItem();
            case MAGENTA -> Blocks.MAGENTA_WOOL.asItem();
            case LIGHT_BLUE -> Blocks.LIGHT_BLUE_WOOL.asItem();
            case YELLOW -> Blocks.YELLOW_WOOL.asItem();
            case LIME -> Blocks.LIME_WOOL.asItem();
            case PINK -> Blocks.PINK_WOOL.asItem();
            case GRAY -> Blocks.GRAY_WOOL.asItem();
            case LIGHT_GRAY -> Blocks.LIGHT_GRAY_WOOL.asItem();
            case CYAN -> Blocks.CYAN_WOOL.asItem();
            case PURPLE -> Blocks.PURPLE_WOOL.asItem();
            case BLUE -> Blocks.BLUE_WOOL.asItem();
            case BROWN -> Blocks.BROWN_WOOL.asItem();
            case GREEN -> Blocks.GREEN_WOOL.asItem();
            case RED -> Blocks.RED_WOOL.asItem();
            case BLACK -> Blocks.BLACK_WOOL.asItem();
            default -> Blocks.WHITE_WOOL.asItem();
        };
    }
}
