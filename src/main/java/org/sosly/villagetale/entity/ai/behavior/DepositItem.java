package org.sosly.villagetale.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import java.util.HashMap;
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
import org.jetbrains.annotations.NotNull;
import org.sosly.villagetale.VillageTale;
import org.sosly.villagetale.api.IVillageZone;
import org.sosly.villagetale.config.CommonConfig;
import org.sosly.villagetale.entity.MemoryModuleTypes;
import org.sosly.villagetale.entity.Villager;
import org.sosly.villagetale.helper.ContainerHelper;
import org.sosly.villagetale.helper.VillagesHelper;
import org.sosly.villagetale.zone.type.Storage;

public class DepositItem extends Behavior<Villager> {
    private static final int BEHAVIOR_DURATION = 200;
    private static final int CLAIM_DURATION = 60;
    private static final int SEARCH_DURATION = 20;

    private BlockPos targetContainer;
    private boolean containerClaimed;
    private int searchTicks;
    private IVillageZone claimedZone;

    public DepositItem() {
        super(ImmutableMap.of(
            MemoryModuleTypes.ITEMS_TO_DEPOSIT.get(), MemoryStatus.VALUE_PRESENT,
            MemoryModuleTypes.VILLAGE.get(), MemoryStatus.VALUE_PRESENT,
            MemoryModuleTypes.BUSY.get(), MemoryStatus.VALUE_ABSENT
        ), BEHAVIOR_DURATION);
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, Villager villager) {
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
    protected void start(@NotNull ServerLevel level, Villager villager, long gameTime) {
        this.containerClaimed = false;
        this.searchTicks = 0;
        this.claimedZone = null;

        villager.getBrain().setMemoryWithExpiry(MemoryModuleTypes.BUSY.get(), true, BEHAVIOR_DURATION);

        if (this.targetContainer != null) {
            villager.getBrain().setMemoryWithExpiry(MemoryModuleType.WALK_TARGET,
                new WalkTarget(this.targetContainer, 0.5F, 1), 200L);

            VillageTale.LOGGER.debug("DepositItem started walking to {} for villager {}",
                this.targetContainer, villager.getId());
        }
    }

    @Override
    protected boolean canStillUse(@NotNull ServerLevel level, @NotNull Villager villager, long gameTime) {
        return this.targetContainer != null &&
               villager.getBrain().hasMemoryValue(MemoryModuleTypes.ITEMS_TO_DEPOSIT.get());
    }

    @Override
    protected void tick(@NotNull ServerLevel level, @NotNull Villager villager, long gameTime) {
        if (this.targetContainer == null) {
            return;
        }

        if (!villager.blockPosition().closerThan(this.targetContainer, CommonConfig.interactionDistance)) {
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

        Map<ResourceLocation, Integer> remainingItems = villager.getBrain()
            .getMemory(MemoryModuleTypes.ITEMS_TO_DEPOSIT.get()).orElse(null);

        if (remainingItems == null || remainingItems.isEmpty()) {
            clearMemories(villager);
            stopBehavior(villager);
            return;
        }

        this.targetContainer = findAvailableContainer(level, villager,
            villager.getBrain().getMemory(MemoryModuleTypes.VILLAGE.get()).orElse(null),
            remainingItems);

        if (this.targetContainer == null) {
            stopBehavior(villager);
            return;
        }

        resetForNewContainer(villager);
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
        villager.getBrain().eraseMemory(MemoryModuleTypes.BUSY.get());
        releaseContainer();
        resetState();
    }

    private boolean isAtStorageZone(ServerLevel level, Villager villager, UUID villageId) {
        return VillagesHelper.getZoneAtPosition(level, villageId, villager.blockPosition(), Storage.ID) != null;
    }

    private BlockPos findAvailableContainer(ServerLevel level, Villager villager, UUID villageId, Map<ResourceLocation, Integer> itemsToDeposit) {
        if (villageId == null) {
            return null;
        }

        IVillageZone zone = VillagesHelper.getZoneAtPosition(level, villageId, villager.blockPosition(), Storage.ID);
        if (zone == null) {
            return null;
        }

        Map<BlockPos, Optional<UUID>> claims = zone.getClaims(level.getGameTime());
        if (claims.isEmpty()) {
            return null;
        }

        for (BlockPos containerPos : claims.keySet()) {
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

        IVillageZone zone = VillagesHelper.getZoneAtPosition(level, villageId, villager.blockPosition(), Storage.ID);
        if (zone == null) {
            return false;
        }

        boolean claimed = zone.claim(this.targetContainer, villager.getUUID(), CLAIM_DURATION, gameTime);
        if (!claimed) {
            VillageTale.LOGGER.debug("DepositItem failed to claim container at {} for villager {}",
                this.targetContainer, villager.getId());
            return false;
        }

        this.containerClaimed = true;
        this.claimedZone = zone;

        VillageTale.LOGGER.debug("DepositItem claimed container at {} for villager {}",
            this.targetContainer, villager.getId());

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

            VillageTale.LOGGER.debug("DepositItem deposited {} x{} for villager {}",
                itemId, deposited, villager.getId());
        }

        if (updatedItems.isEmpty()) {
            villager.getBrain().eraseMemory(MemoryModuleTypes.ITEMS_TO_DEPOSIT.get());
        } else {
            villager.getBrain().setMemoryWithExpiry(MemoryModuleTypes.ITEMS_TO_DEPOSIT.get(), updatedItems, 1200L);
        }
    }

    private void releaseContainer() {
        if (!this.containerClaimed || this.claimedZone == null || this.targetContainer == null) {
            return;
        }


        boolean released = this.claimedZone.release(this.targetContainer);

        VillageTale.LOGGER.debug("DepositItem {} container at {}",
            released ? "released" : "failed to release", this.targetContainer);
    }

    private void clearMemories(Villager villager) {
        villager.getBrain().eraseMemory(MemoryModuleTypes.ITEMS_TO_DEPOSIT.get());
    }

    private void resetState() {
        this.targetContainer = null;
        this.containerClaimed = false;
        this.searchTicks = 0;
        this.claimedZone = null;
    }

    private void resetForNewContainer(Villager villager) {
        releaseContainer();
        this.containerClaimed = false;
        this.searchTicks = 0;
        this.claimedZone = null;

        villager.getBrain().setMemoryWithExpiry(MemoryModuleType.WALK_TARGET,
            new WalkTarget(this.targetContainer, 0.5F, 1), 200L);
    }

    private void stopBehavior(Villager villager) {
        villager.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
    }
}
