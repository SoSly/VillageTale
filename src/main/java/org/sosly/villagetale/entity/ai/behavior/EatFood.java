package org.sosly.villagetale.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.SimpleContainer;
import org.sosly.villagetale.api.IWantedItem;
import org.sosly.villagetale.entity.Villager;
import org.sosly.villagetale.entity.MemoryModuleTypes;
import org.sosly.villagetale.helper.ItemMatcher;
import org.sosly.villagetale.network.NetworkHandler;

public class EatFood extends Behavior<Villager> {
    private static final int EATING_DURATION = 32;
    private static final int EATING_THRESHOLD = 18;

    private ItemStack foodToEat;
    private int eatingTime;

    public EatFood() {
        super(ImmutableMap.of(
            MemoryModuleTypes.CAN_EAT.get(), MemoryStatus.VALUE_PRESENT,
            MemoryModuleTypes.BUSY.get(), MemoryStatus.VALUE_ABSENT
        ), EATING_DURATION);
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, Villager villager) {

        Boolean canEat = villager.getBrain().getMemory(MemoryModuleTypes.CAN_EAT.get()).orElse(false);
        if (!canEat) {
            return false;
        }

        int foodLevel = villager.getFoodData().getFoodLevel();
        if (foodLevel >= EATING_THRESHOLD) {
            return false;
        }

        ItemStack bestFood = findBestFood(villager);
        if (bestFood.isEmpty()) {
            return false;
        }

        this.foodToEat = bestFood.copy();
        return true;
    }

    @Override
    protected void start(ServerLevel level, Villager villager, long gameTime) {
        this.eatingTime = 0;
        ItemStack foodCopy = this.foodToEat.copy();
        villager.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, foodCopy);
        NetworkHandler.syncEquipmentToNearbyPlayers(villager, net.minecraft.world.InteractionHand.MAIN_HAND, foodCopy);
        villager.startUsingItem(net.minecraft.world.InteractionHand.MAIN_HAND);
        playEatingSound(level, villager);
    }

    @Override
    protected boolean canStillUse(ServerLevel level, Villager villager, long gameTime) {
        return this.eatingTime < EATING_DURATION && !this.foodToEat.isEmpty();
    }

    @Override
    protected void tick(ServerLevel level, Villager villager, long gameTime) {
        this.eatingTime++;

        if (this.eatingTime % 4 == 0) {
            playEatingSound(level, villager);
        }
    }

    @Override
    protected void stop(ServerLevel level, Villager villager, long gameTime) {
        villager.stopUsingItem();
        villager.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        NetworkHandler.syncEquipmentToNearbyPlayers(villager, net.minecraft.world.InteractionHand.MAIN_HAND, ItemStack.EMPTY);

        if (this.foodToEat.isEmpty()) {
            this.foodToEat = ItemStack.EMPTY;
            this.eatingTime = 0;
            return;
        }

        FoodProperties foodProperties = this.foodToEat.getFoodProperties(villager);
        if (foodProperties == null) {
            this.foodToEat = ItemStack.EMPTY;
            this.eatingTime = 0;
            return;
        }

        villager.getFoodData().eat(foodProperties.getNutrition(), foodProperties.getSaturationModifier());

        SimpleContainer inventory = villager.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack inventoryItem = inventory.getItem(i);
            if (!inventoryItem.isEmpty() && ItemStack.isSameItemSameTags(inventoryItem, this.foodToEat)) {
                inventoryItem.shrink(1);
                break;
            }
        }

        this.foodToEat = ItemStack.EMPTY;
        this.eatingTime = 0;
    }

    private ItemStack findBestFood(Villager villager) {
        SimpleContainer inventory = villager.getInventory();
        IWantedItem foodMatcher = ItemMatcher.FOOD.getFor(villager);

        ItemStack bestFood = ItemStack.EMPTY;
        float bestSaturation = 0.0f;

        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack item = inventory.getItem(i);
            if (item.isEmpty()) {
                continue;
            }

            if (!foodMatcher.getMatcher().test(item)) {
                continue;
            }

            FoodProperties foodProperties = item.getFoodProperties(villager);
            if (foodProperties == null) {
                continue;
            }

            float saturation = foodProperties.getSaturationModifier();
            if (saturation > bestSaturation) {
                bestFood = item;
                bestSaturation = saturation;
            }
        }

        return bestFood;
    }

    private void playEatingSound(ServerLevel level, Villager villager) {
        villager.playSound(SoundEvents.GENERIC_EAT, 0.5F + 0.5F * level.random.nextInt(2),
            (level.random.nextFloat() - level.random.nextFloat()) * 0.2F + 1.0F);
    }
}
