package org.sosly.villageworks.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.SimpleContainer;
import org.sosly.villageworks.entity.Villager;
import org.sosly.villageworks.registry.MemoryModuleTypes;

public class EatFood extends Behavior<Villager> {
    private static final int EATING_DURATION = 32;
    private static final int EATING_THRESHOLD = 18;
    
    private ItemStack foodToEat;
    private int eatingTime;
    
    public EatFood() {
        super(ImmutableMap.of(
            MemoryModuleTypes.CAN_EAT.get(), MemoryStatus.VALUE_PRESENT
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
        villager.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, this.foodToEat.copy());
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
        ItemStack inventoryItem = inventory.getItem(0);
        if (!inventoryItem.isEmpty() && inventoryItem.is(this.foodToEat.getItem())) {
            inventoryItem.shrink(1);
        }
        
        this.foodToEat = ItemStack.EMPTY;
        this.eatingTime = 0;
    }
    
    private ItemStack findBestFood(Villager villager) {
        SimpleContainer inventory = villager.getInventory();
        ItemStack item = inventory.getItem(0);
        
        if (item.isEmpty()) {
            return ItemStack.EMPTY;
        }
        
        FoodProperties foodProperties = item.getFoodProperties(villager);
        if (foodProperties == null) {
            return ItemStack.EMPTY;
        }
        
        return item;
    }
    
    private void playEatingSound(ServerLevel level, Villager villager) {
        villager.playSound(SoundEvents.GENERIC_EAT, 0.5F + 0.5F * level.random.nextInt(2),
            (level.random.nextFloat() - level.random.nextFloat()) * 0.2F + 1.0F);
    }
}