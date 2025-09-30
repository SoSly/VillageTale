package org.sosly.villagetale.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import net.minecraft.core.BlockPos;
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
import org.jetbrains.annotations.NotNull;
import org.sosly.villagetale.api.IVillageZone;
import org.sosly.villagetale.config.CommonConfig;
import org.sosly.villagetale.entity.MemoryModuleTypes;
import org.sosly.villagetale.entity.Villager;
import org.sosly.villagetale.helper.InventoryHelper;
import org.sosly.villagetale.helper.VillagesHelper;
import org.sosly.villagetale.network.NetworkHandler;

import java.util.Optional;
import java.util.UUID;

public class BringAnimalsToPen extends Behavior<Villager> {
    private static final int BEHAVIOR_DURATION = 1200;
    private static final float WORK_EXHAUSTION = 0.8f;
    private static final double MAX_LEASH_DISTANCE = 10.0;
    private static final double LEASH_ATTACH_DISTANCE = 3.0;
    
    private enum Phase {
        APPROACHING,
        LEASHING,
        LEADING,
        RELEASING
    }
    
    private boolean claimed;
    private Animal targetAnimal;
    private UUID animalId;
    private IVillageZone pen;
    private Phase currentPhase;
    private BlockPos penCenter;
    private int phaseTicks;
    private boolean hasLeadAttached;
    
    public BringAnimalsToPen() {
        super(ImmutableMap.of(
                MemoryModuleTypes.WORK_ZONE.get(), MemoryStatus.VALUE_PRESENT,
                MemoryModuleTypes.WANDERING_ANIMAL.get(), MemoryStatus.VALUE_PRESENT,
                MemoryModuleTypes.BUSY.get(), MemoryStatus.VALUE_ABSENT
        ), BEHAVIOR_DURATION);
    }
    
    @Override
    protected boolean checkExtraStartConditions(@NotNull ServerLevel level, @NotNull Villager villager) {
        if (InventoryHelper.getItem(villager, stack -> stack.is(Items.LEAD)).isEmpty()) {
            return false;
        }
        
        IVillageZone zone = VillagesHelper.getWorkplaceZone(level, villager);
        if (zone == null) {
            return false;
        }
        
        if (!zone.containsPosition(villager.blockPosition())) {
            return false;
        }
        
        Optional<Entity> wanderingMemory = villager.getBrain().getMemory(MemoryModuleTypes.WANDERING_ANIMAL.get());
        if (wanderingMemory.isEmpty() || !(wanderingMemory.get() instanceof Animal)) {
            return false;
        }
        
        Animal animal = (Animal) wanderingMemory.get();
        if (!animal.isAlive() || animal.getLeashHolder() != null) {
            return false;
        }
        
        if (zone.containsPosition(animal.blockPosition())) {
            return false;
        }
        
        this.pen = zone;
        return true;
    }
    
    @Override
    protected void start(@NotNull ServerLevel level, Villager villager, long gameTime) {
        Optional<Entity> wanderingMemory = villager.getBrain().getMemory(MemoryModuleTypes.WANDERING_ANIMAL.get());
        if (wanderingMemory.isEmpty() || !(wanderingMemory.get() instanceof Animal)) {
            return;
        }
        
        Animal animal = (Animal) wanderingMemory.get();
        
        if (!pen.claim(animal.getUUID(), villager.getUUID(), BEHAVIOR_DURATION, gameTime)) {
            return;
        }
        
        targetAnimal = animal;
        animalId = animal.getUUID();
        claimed = true;
        currentPhase = Phase.APPROACHING;
        phaseTicks = 0;
        hasLeadAttached = false;
        
        penCenter = pen.getStartPosition();
        
        ItemStack lead = InventoryHelper.getItem(villager, stack -> stack.is(Items.LEAD));
        if (lead.isEmpty()) {
            pen.release(animalId);
            claimed = false;
            return;
        }
        
        villager.getBrain().setMemoryWithExpiry(MemoryModuleTypes.BUSY.get(), true, BEHAVIOR_DURATION);
        villager.setItemInHand(InteractionHand.MAIN_HAND, lead.copy());
        NetworkHandler.syncEquipmentToNearbyPlayers(villager, InteractionHand.MAIN_HAND, lead.copy());
        
        villager.getBrain().setMemory(MemoryModuleType.WALK_TARGET,
            new WalkTarget(targetAnimal.blockPosition(), 0.5f, 2));
    }
    
    @Override
    protected boolean canStillUse(@NotNull ServerLevel level, @NotNull Villager villager, long gameTime) {
        if (!claimed || targetAnimal == null || !targetAnimal.isAlive()) {
            return false;
        }
        
        if (currentPhase == Phase.RELEASING && pen.containsPosition(targetAnimal.blockPosition())) {
            return true;
        }
        
        if (villager.distanceToSqr(targetAnimal) > 225) {
            return false;
        }
        
        return phaseTicks < BEHAVIOR_DURATION;
    }
    
    @Override
    protected void tick(@NotNull ServerLevel level, @NotNull Villager villager, long gameTime) {
        if (!claimed || targetAnimal == null) {
            return;
        }
        
        phaseTicks++;
        
        switch (currentPhase) {
            case APPROACHING -> tickApproaching(level, villager);
            case LEASHING -> tickLeashing(level, villager);
            case LEADING -> tickLeading(level, villager);
            case RELEASING -> tickReleasing(level, villager);
        }
    }
    
    private void tickApproaching(ServerLevel level, Villager villager) {
        if (villager.distanceToSqr(targetAnimal) > LEASH_ATTACH_DISTANCE * LEASH_ATTACH_DISTANCE) {
            villager.getBrain().setMemory(MemoryModuleType.WALK_TARGET,
                new WalkTarget(targetAnimal.blockPosition(), 0.5f, 2));
            return;
        }
        
        villager.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
        currentPhase = Phase.LEASHING;
        phaseTicks = 0;
    }
    
    private void tickLeashing(ServerLevel level, Villager villager) {
        villager.getLookControl().setLookAt(targetAnimal);
        
        if (phaseTicks == 10) {
            villager.swing(InteractionHand.MAIN_HAND);
            
            ItemStack lead = InventoryHelper.getItem(villager, stack -> stack.is(Items.LEAD));
            if (lead.isEmpty()) {
                return;
            }
            
            targetAnimal.setLeashedTo(villager, true);
            hasLeadAttached = true;
            lead.shrink(1);
            
            level.playSound(null, targetAnimal.blockPosition(), 
                SoundEvents.LEASH_KNOT_PLACE, SoundSource.NEUTRAL, 0.5F, 1.0F);
            
            currentPhase = Phase.LEADING;
            phaseTicks = 0;
        }
    }
    
    private void tickLeading(ServerLevel level, Villager villager) {
        if (!hasLeadAttached || targetAnimal.getLeashHolder() != villager) {
            if (villager.distanceToSqr(targetAnimal) <= LEASH_ATTACH_DISTANCE * LEASH_ATTACH_DISTANCE) {
                ItemStack lead = InventoryHelper.getItem(villager, stack -> stack.is(Items.LEAD));
                if (!lead.isEmpty()) {
                    targetAnimal.setLeashedTo(villager, true);
                    hasLeadAttached = true;
                    lead.shrink(1);
                }
            } else {
                currentPhase = Phase.APPROACHING;
                phaseTicks = 0;
                return;
            }
        }
        
        if (villager.distanceToSqr(targetAnimal) > MAX_LEASH_DISTANCE * MAX_LEASH_DISTANCE) {
            villager.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
            return;
        }
        
        if (pen.containsPosition(targetAnimal.blockPosition())) {
            currentPhase = Phase.RELEASING;
            phaseTicks = 0;
            villager.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
            return;
        }
        
        if (!villager.getBrain().hasMemoryValue(MemoryModuleType.WALK_TARGET) || phaseTicks % 40 == 0) {
            BlockPos targetPos = findBestPathToPen(villager.blockPosition(), penCenter);
            villager.getBrain().setMemory(MemoryModuleType.WALK_TARGET,
                new WalkTarget(targetPos, 0.5f, 1));
        }
    }
    
    private void tickReleasing(ServerLevel level, Villager villager) {
        if (phaseTicks == 10) {
            if (hasLeadAttached && targetAnimal.getLeashHolder() == villager) {
                targetAnimal.setLeashedTo(null, true);
                
                ItemStack leadDrop = new ItemStack(Items.LEAD);
                if (!villager.getInventory().canAddItem(leadDrop)) {
                    villager.spawnAtLocation(leadDrop);
                } else {
                    villager.getInventory().addItem(leadDrop);
                }
                
                level.playSound(null, targetAnimal.blockPosition(), 
                    SoundEvents.LEASH_KNOT_BREAK, SoundSource.NEUTRAL, 0.5F, 1.0F);
            }
            
            villager.getFoodData().addExhaustion(WORK_EXHAUSTION);
            phaseTicks = BEHAVIOR_DURATION;
        }
    }
    
    private BlockPos findBestPathToPen(BlockPos from, BlockPos penCenter) {
        double distance = from.distSqr(penCenter);
        if (distance < 16) {
            return penCenter;
        }
        
        double ratio = Math.sqrt(16 / distance);
        int targetX = from.getX() + (int)((penCenter.getX() - from.getX()) * ratio);
        int targetZ = from.getZ() + (int)((penCenter.getZ() - from.getZ()) * ratio);
        
        return new BlockPos(targetX, from.getY(), targetZ);
    }
    
    @Override
    protected void stop(@NotNull ServerLevel level, @NotNull Villager villager, long gameTime) {
        if (hasLeadAttached && targetAnimal != null && targetAnimal.getLeashHolder() == villager) {
            targetAnimal.setLeashedTo(null, true);
            
            ItemStack leadDrop = new ItemStack(Items.LEAD);
            if (!villager.getInventory().canAddItem(leadDrop)) {
                villager.spawnAtLocation(leadDrop);
            } else {
                villager.getInventory().addItem(leadDrop);
            }
        }
        
        if (claimed && animalId != null) {
            pen.release(animalId);
        }
        
        claimed = false;
        targetAnimal = null;
        animalId = null;
        currentPhase = null;
        phaseTicks = 0;
        hasLeadAttached = false;
        penCenter = null;
        
        villager.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        NetworkHandler.syncEquipmentToNearbyPlayers(villager, InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        villager.getBrain().eraseMemory(MemoryModuleTypes.BUSY.get());
        villager.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
        villager.getBrain().eraseMemory(MemoryModuleTypes.WANDERING_ANIMAL.get());
    }
}