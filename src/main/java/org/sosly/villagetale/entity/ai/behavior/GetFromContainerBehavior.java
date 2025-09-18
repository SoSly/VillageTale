package org.sosly.villagetale.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
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
import org.sosly.villagetale.capability.Capabilities;
import org.sosly.villagetale.data.FoundItem;
import org.sosly.villagetale.data.VillageInfo;
import org.sosly.villagetale.data.WantedItem;
import org.sosly.villagetale.entity.MemoryModuleTypes;
import org.sosly.villagetale.entity.Villager;
import org.sosly.villagetale.helper.ContainerHelper;

public class GetFromContainerBehavior extends Behavior<Villager> {
    private static final int BEHAVIOR_DURATION = 100;
    private static final double INTERACTION_DISTANCE = 2.0D;
    private static final int CLAIM_DURATION = 60;
    private static final int SEARCH_DURATION = 20;

    private BlockPos targetContainer;
    private boolean atContainer;
    private boolean containerClaimed;
    private int searchTicks;
    private IVillageZone claimedZone;

    public GetFromContainerBehavior() {
        super(ImmutableMap.of(
            MemoryModuleTypes.FOUND_ITEM.get(), MemoryStatus.VALUE_PRESENT,
            MemoryModuleTypes.WANTED_ITEM.get(), MemoryStatus.VALUE_PRESENT,
            MemoryModuleTypes.VILLAGE.get(), MemoryStatus.VALUE_PRESENT
        ), BEHAVIOR_DURATION);
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, Villager villager) {
        FoundItem foundItem = villager.getBrain().getMemory(MemoryModuleTypes.FOUND_ITEM.get()).orElse(null);
        if (foundItem == null) {
            return false;
        }

        WantedItem wantedItem = villager.getBrain().getMemory(MemoryModuleTypes.WANTED_ITEM.get()).orElse(null);
        if (wantedItem == null) {
            return false;
        }

        this.targetContainer = foundItem.containerPos();
        return true;
    }

    @Override
    protected void start(ServerLevel level, Villager villager, long gameTime) {
        this.atContainer = false;
        this.containerClaimed = false;
        this.searchTicks = 0;
        this.claimedZone = null;

        villager.getBrain().setMemory(MemoryModuleType.WALK_TARGET,
            new WalkTarget(this.targetContainer, 0.5F, 1));

        if (VillageTale.LOGGER.isDebugEnabled()) {
            VillageTale.LOGGER.debug("GetFromContainer started walking to {} for villager {}",
                this.targetContainer, villager.getId());
        }
    }

    @Override
    protected boolean canStillUse(ServerLevel level, Villager villager, long gameTime) {
        return this.targetContainer != null && 
               villager.getBrain().hasMemoryValue(MemoryModuleTypes.FOUND_ITEM.get()) &&
               villager.getBrain().hasMemoryValue(MemoryModuleTypes.WANTED_ITEM.get());
    }

    @Override
    protected void tick(ServerLevel level, Villager villager, long gameTime) {
        if (this.targetContainer == null) {
            return;
        }

        if (!villager.blockPosition().closerThan(this.targetContainer, INTERACTION_DISTANCE)) {
            return;
        }

        if (!this.atContainer) {
            this.atContainer = true;
            this.searchTicks = 0;
            villager.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);

            if (!claimContainer(level, villager, gameTime)) {
                stopBehavior(villager);
                return;
            }

            ContainerHelper.openContainer(level, this.targetContainer);
            return;
        }

        this.searchTicks++;

        if (this.searchTicks < SEARCH_DURATION) {
            return;
        }

        extractItems(level, villager);
        clearMemories(villager);
        stopBehavior(villager);
    }

    @Override
    protected void stop(ServerLevel level, Villager villager, long gameTime) {
        releaseContainer();
        clearMemories(villager);
        resetState();
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
                VillageTale.LOGGER.debug("GetFromContainer failed to claim container at {} for villager {}",
                    this.targetContainer, villager.getId());
            }
            return false;
        }

        this.containerClaimed = true;
        this.claimedZone = zone;

        if (VillageTale.LOGGER.isDebugEnabled()) {
            VillageTale.LOGGER.debug("GetFromContainer claimed container at {} for villager {}",
                this.targetContainer, villager.getId());
        }

        return true;
    }

    private void extractItems(ServerLevel level, Villager villager) {
        WantedItem wantedItem = villager.getBrain().getMemory(MemoryModuleTypes.WANTED_ITEM.get()).orElse(null);
        if (wantedItem == null) {
            return;
        }

        SimpleContainer inventory = villager.getInventory();
        
        if (findFirstEmptySlot(inventory) < 0) {
            if (VillageTale.LOGGER.isDebugEnabled()) {
                VillageTale.LOGGER.debug("GetFromContainer aborting - no empty slots for villager {}", villager.getId());
            }
            return;
        }

        int maxToExtract = wantedItem.getAmount();
        int extracted = 0;

        while (extracted < maxToExtract) {
            int emptySlot = findFirstEmptySlot(inventory);
            if (emptySlot < 0) {
                if (VillageTale.LOGGER.isDebugEnabled()) {
                    VillageTale.LOGGER.debug("GetFromContainer stopping - inventory full for villager {}", villager.getId());
                }
                break;
            }

            ItemStack extractedItem = ContainerHelper.extractItemFromContainer(level, this.targetContainer, wantedItem.getMatcher());
            if (extractedItem.isEmpty()) {
                if (VillageTale.LOGGER.isDebugEnabled()) {
                    VillageTale.LOGGER.debug("GetFromContainer stopping - no more matching items for villager {}", villager.getId());
                }
                break;
            }

            int takeAmount = Math.min(extractedItem.getCount(), maxToExtract - extracted);
            extractedItem.setCount(takeAmount);
            
            inventory.setItem(emptySlot, extractedItem);
            extracted += takeAmount;

            if (VillageTale.LOGGER.isDebugEnabled()) {
                VillageTale.LOGGER.debug("GetFromContainer extracted {} x{} for villager {}",
                    extractedItem.getItem(), takeAmount, villager.getId());
            }
        }

        if (extracted > 0) {
            if (VillageTale.LOGGER.isDebugEnabled()) {
                VillageTale.LOGGER.debug("GetFromContainer total extracted: {} items for villager {}",
                    extracted, villager.getId());
            }
        } else if (VillageTale.LOGGER.isDebugEnabled()) {
            VillageTale.LOGGER.debug("GetFromContainer extracted nothing for villager {}", villager.getId());
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
                VillageTale.LOGGER.debug("GetFromContainer {} container at {}",
                    released ? "released" : "failed to release", this.targetContainer);
            }
        }
    }

    private void clearMemories(Villager villager) {
        villager.getBrain().eraseMemory(MemoryModuleTypes.WANTED_ITEM.get());
        villager.getBrain().eraseMemory(MemoryModuleTypes.ALREADY_SCANNED_STORAGES.get());
        villager.getBrain().eraseMemory(MemoryModuleTypes.FOUND_ITEM.get());
    }

    private void resetState() {
        this.targetContainer = null;
        this.atContainer = false;
        this.containerClaimed = false;
        this.searchTicks = 0;
        this.claimedZone = null;
    }

    private void stopBehavior(Villager villager) {
        villager.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
    }

    private int findFirstEmptySlot(SimpleContainer inventory) {
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            if (inventory.getItem(i).isEmpty()) {
                return i;
            }
        }
        return -1;
    }
}