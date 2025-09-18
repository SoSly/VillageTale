package org.sosly.villagetale.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import org.sosly.villagetale.VillageTale;
import org.sosly.villagetale.api.capability.IVillageCapability;
import org.sosly.villagetale.api.capability.IVillagesCapability;
import org.sosly.villagetale.api.data.IVillageZone;
import org.sosly.villagetale.api.data.ZoneType;
import org.sosly.villagetale.capability.Capabilities;
import org.sosly.villagetale.data.VillageInfo;
import org.sosly.villagetale.entity.MemoryModuleTypes;
import org.sosly.villagetale.entity.Villager;
import org.sosly.villagetale.helper.ContainerHelper;

public class DepositItem extends Behavior<Villager> {
    private static final int BEHAVIOR_DURATION = 200;
    private static final double INTERACTION_DISTANCE = 2.0D;
    private static final int CLAIM_DURATION = 60;
    private static final int SEARCH_DURATION = 20;

    private BlockPos targetContainer;
    private boolean atStorageZone;
    private boolean containerClaimed;
    private int searchTicks;
    private IVillageZone claimedZone;

    public DepositItem() {
        super(ImmutableMap.of(
            MemoryModuleTypes.ITEMS_TO_DEPOSIT.get(), MemoryStatus.VALUE_PRESENT,
            MemoryModuleTypes.VILLAGE.get(), MemoryStatus.VALUE_PRESENT
        ), BEHAVIOR_DURATION);
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, Villager villager) {
        @SuppressWarnings("unchecked")
        Map<ResourceLocation, Integer> itemsToDeposit = villager.getBrain()
            .getMemory(MemoryModuleTypes.ITEMS_TO_DEPOSIT.get()).orElse(null);
        
        if (itemsToDeposit == null || itemsToDeposit.isEmpty()) {
            return false;
        }

        UUID villageId = villager.getBrain().getMemory(MemoryModuleTypes.VILLAGE.get()).orElse(null);
        if (villageId == null) {
            return false;
        }

        if (!isAtStorageZone(level, villager, villageId)) {
            return false;
        }

        this.targetContainer = findAvailableContainer(level, villager, villageId, itemsToDeposit);
        return this.targetContainer != null;
    }

    @Override
    protected void start(ServerLevel level, Villager villager, long gameTime) {
        this.atStorageZone = true;
        this.containerClaimed = false;
        this.searchTicks = 0;
        this.claimedZone = null;

        if (this.targetContainer != null) {
            villager.getBrain().setMemory(MemoryModuleType.WALK_TARGET,
                new WalkTarget(this.targetContainer, 0.5F, 1));

            if (VillageTale.LOGGER.isDebugEnabled()) {
                VillageTale.LOGGER.debug("DepositItem started walking to {} for villager {}",
                    this.targetContainer, villager.getId());
            }
        }
    }

    @Override
    protected boolean canStillUse(ServerLevel level, Villager villager, long gameTime) {
        return this.targetContainer != null &&
               villager.getBrain().hasMemoryValue(MemoryModuleTypes.ITEMS_TO_DEPOSIT.get());
    }

    @Override
    protected void tick(ServerLevel level, Villager villager, long gameTime) {
        if (this.targetContainer == null) {
            return;
        }

        if (!villager.blockPosition().closerThan(this.targetContainer, INTERACTION_DISTANCE)) {
            return;
        }

        if (!this.containerClaimed) {
            handleArrival(level, villager, gameTime);
            return;
        }

        this.searchTicks++;

        if (this.searchTicks < SEARCH_DURATION) {
            return;
        }

        depositItems(level, villager);
        
        @SuppressWarnings("unchecked")
        Map<ResourceLocation, Integer> remainingItems = villager.getBrain()
            .getMemory(MemoryModuleTypes.ITEMS_TO_DEPOSIT.get()).orElse(null);
        
        if (remainingItems == null || remainingItems.isEmpty()) {
            clearMemories(villager);
            stopBehavior(villager);
        } else {
            this.targetContainer = findAvailableContainer(level, villager, 
                villager.getBrain().getMemory(MemoryModuleTypes.VILLAGE.get()).orElse(null), 
                remainingItems);
            
            if (this.targetContainer == null) {
                stopBehavior(villager);
            } else {
                resetForNewContainer(villager);
            }
        }
    }

    private void handleArrival(ServerLevel level, Villager villager, long gameTime) {
        this.searchTicks = 0;
        villager.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);

        if (!claimContainer(level, villager, gameTime)) {
            stopBehavior(villager);
            return;
        }

        ContainerHelper.openContainer(level, this.targetContainer);
    }

    @Override
    protected void stop(ServerLevel level, Villager villager, long gameTime) {
        releaseContainer();
        resetState();
    }

    private boolean isAtStorageZone(ServerLevel level, Villager villager, UUID villageId) {
        IVillageZone zone = findZoneContaining(level, villageId, villager.blockPosition());
        return zone != null && zone.getType() == ZoneType.STORAGE;
    }

    private BlockPos findAvailableContainer(ServerLevel level, Villager villager, UUID villageId, Map<ResourceLocation, Integer> itemsToDeposit) {
        if (villageId == null) {
            return null;
        }

        IVillageZone zone = findZoneContaining(level, villageId, villager.blockPosition());
        if (zone == null || zone.getType() != ZoneType.STORAGE) {
            return null;
        }

        Optional<List<BlockPos>> pois = zone.getPOIs();
        if (pois.isEmpty()) {
            return null;
        }

        for (BlockPos containerPos : pois.get()) {
            for (ResourceLocation itemId : itemsToDeposit.keySet()) {
                net.minecraft.world.item.Item item = BuiltInRegistries.ITEM.get(itemId);
                
                if (ContainerHelper.hasAvailableSpace(level, containerPos, item)) {
                    return containerPos;
                }
            }
        }

        return null;
    }

    private boolean claimContainer(ServerLevel level, Villager villager, long gameTime) {
        UUID villageId = villager.getBrain().getMemory(MemoryModuleTypes.VILLAGE.get()).orElse(null);
        if (villageId == null) {
            return false;
        }

        IVillageZone zone = findZoneContaining(level, villageId, this.targetContainer);
        if (zone == null) {
            return false;
        }

        boolean claimed = zone.claim(this.targetContainer, villager.getUUID(), CLAIM_DURATION, gameTime);
        if (!claimed) {
            if (VillageTale.LOGGER.isDebugEnabled()) {
                VillageTale.LOGGER.debug("DepositItem failed to claim container at {} for villager {}",
                    this.targetContainer, villager.getId());
            }
            return false;
        }

        this.containerClaimed = true;
        this.claimedZone = zone;

        if (VillageTale.LOGGER.isDebugEnabled()) {
            VillageTale.LOGGER.debug("DepositItem claimed container at {} for villager {}",
                this.targetContainer, villager.getId());
        }

        return true;
    }

    private void depositItems(ServerLevel level, Villager villager) {
        @SuppressWarnings("unchecked")
        Map<ResourceLocation, Integer> itemsToDeposit = villager.getBrain()
            .getMemory(MemoryModuleTypes.ITEMS_TO_DEPOSIT.get()).orElse(null);
        
        if (itemsToDeposit == null || itemsToDeposit.isEmpty()) {
            return;
        }

        SimpleContainer inventory = villager.getInventory();
        Map<ResourceLocation, Integer> updatedItems = new HashMap<>(itemsToDeposit);

        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }

            ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
            Integer wantedAmount = itemsToDeposit.get(itemId);
            if (wantedAmount == null || wantedAmount <= 0) {
                continue;
            }

            int deposited = ContainerHelper.depositItemToContainer(level, this.targetContainer, stack, wantedAmount);
            if (deposited <= 0) {
                continue;
            }

            stack.shrink(deposited);
            int remaining = wantedAmount - deposited;
            
            if (remaining <= 0) {
                updatedItems.remove(itemId);
            } else {
                updatedItems.put(itemId, remaining);
            }

            if (VillageTale.LOGGER.isDebugEnabled()) {
                VillageTale.LOGGER.debug("DepositItem deposited {} x{} for villager {}",
                    itemId, deposited, villager.getId());
            }
        }

        if (updatedItems.isEmpty()) {
            villager.getBrain().eraseMemory(MemoryModuleTypes.ITEMS_TO_DEPOSIT.get());
        } else {
            villager.getBrain().setMemory(MemoryModuleTypes.ITEMS_TO_DEPOSIT.get(), updatedItems);
        }
    }

    private IVillageZone findZoneContaining(ServerLevel level, UUID villageId, BlockPos pos) {
        IVillagesCapability villagesCapability = level.getCapability(Capabilities.VILLAGES_CAPABILITY).orElse(null);
        if (villagesCapability == null) {
            return null;
        }

        VillageInfo village = villagesCapability.getVillageById(villageId);
        if (village == null) {
            return null;
        }

        ChunkPos townHallChunk = new ChunkPos(village.getTownHallPos());
        LevelChunk chunk = level.getChunk(townHallChunk.x, townHallChunk.z);

        IVillageCapability villageCapability = chunk.getCapability(Capabilities.VILLAGE_CAPABILITY).orElse(null);
        if (villageCapability == null) {
            return null;
        }

        return villageCapability.getZones()
                .stream()
                .filter(zone -> {
                    Optional<List<BlockPos>> pois = zone.getPOIs();
                    return pois.map(list -> list.contains(pos)).orElse(false);
                })
                .findFirst()
                .orElse(null);
    }

    private void releaseContainer() {
        if (this.containerClaimed && this.claimedZone != null && this.targetContainer != null) {
            boolean released = this.claimedZone.release(this.targetContainer);

            if (VillageTale.LOGGER.isDebugEnabled()) {
                VillageTale.LOGGER.debug("DepositItem {} container at {}",
                    released ? "released" : "failed to release", this.targetContainer);
            }
        }
    }

    private void clearMemories(Villager villager) {
        villager.getBrain().eraseMemory(MemoryModuleTypes.ITEMS_TO_DEPOSIT.get());
    }

    private void resetState() {
        this.targetContainer = null;
        this.atStorageZone = false;
        this.containerClaimed = false;
        this.searchTicks = 0;
        this.claimedZone = null;
    }

    private void resetForNewContainer(Villager villager) {
        releaseContainer();
        this.containerClaimed = false;
        this.searchTicks = 0;
        this.claimedZone = null;

        villager.getBrain().setMemory(MemoryModuleType.WALK_TARGET,
            new WalkTarget(this.targetContainer, 0.5F, 1));
    }

    private void stopBehavior(Villager villager) {
        villager.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
    }
}