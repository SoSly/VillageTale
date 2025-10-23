package org.sosly.villagetale.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.chunk.LevelChunk;
import org.sosly.villagetale.VillageTale;
import org.sosly.villagetale.api.IRecipeManager;
import org.sosly.villagetale.api.IVillageZone;
import org.sosly.villagetale.api.capability.IVillageCapability;
import org.sosly.villagetale.api.capability.IVillagesCapability;
import org.sosly.villagetale.capability.Capabilities;
import org.sosly.villagetale.config.CommonConfig;
import org.sosly.villagetale.data.matchers.ItemOrTagMatcher;
import org.sosly.villagetale.data.VillageInfo;
import org.sosly.villagetale.entity.MemoryModuleTypes;
import org.sosly.villagetale.entity.Villager;
import org.sosly.villagetale.helper.ContainerHelper;
import org.sosly.villagetale.data.RecipeManager;

public class RefuelWorkstation extends Behavior<Villager> {
    private static final int BEHAVIOR_DURATION = 100;
    private static final int CLAIM_DURATION = 60;
    private static final int SEARCH_DURATION = 40;
    private static final int MAX_FUEL_TO_PLACE = 3;

    private BlockPos targetWorkstation;
    private boolean atWorkstation;
    private boolean workstationClaimed;
    private int searchTicks;
    private IVillageZone claimedZone;

    public RefuelWorkstation() {
        super(ImmutableMap.of(
            MemoryModuleTypes.WORKSTATION_NEEDS_FUEL.get(), MemoryStatus.VALUE_PRESENT,
            MemoryModuleTypes.BUSY.get(), MemoryStatus.VALUE_ABSENT
        ), BEHAVIOR_DURATION);
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, Villager villager) {
        BlockPos workstation = villager.getBrain().getMemory(MemoryModuleTypes.WORKSTATION_NEEDS_FUEL.get()).orElse(null);
        if (workstation == null) {
            return false;
        }

        Block block = level.getBlockState(workstation).getBlock();
        IRecipeManager recipeManager = RecipeManager.getInstance();

        ItemOrTagMatcher fuelMatcher = recipeManager.getFuelItemsForBlock(block).orElse(null);
        if (fuelMatcher == null) {
            return false;
        }

        if (!hasFuel(villager, fuelMatcher)) {
            return false;
        }

        this.targetWorkstation = workstation;
        return true;
    }

    @Override
    protected void start(ServerLevel level, Villager villager, long gameTime) {
        this.atWorkstation = false;
        this.workstationClaimed = false;
        this.searchTicks = 0;
        this.claimedZone = null;

        villager.getBrain().setMemoryWithExpiry(MemoryModuleTypes.BUSY.get(), true, BEHAVIOR_DURATION);
        int closeEnoughDistance = (int) (CommonConfig.interactionDistance / 2);
        villager.getBrain().setMemoryWithExpiry(MemoryModuleType.WALK_TARGET,
            new WalkTarget(this.targetWorkstation, 0.5F, closeEnoughDistance), 200L);

        VillageTale.LOGGER.debug("RefuelWorkstation started walking to {} for villager {}",
            this.targetWorkstation, villager.getId());
    }

    @Override
    protected boolean canStillUse(ServerLevel level, Villager villager, long gameTime) {
        return this.targetWorkstation != null &&
               villager.getBrain().hasMemoryValue(MemoryModuleTypes.WORKSTATION_NEEDS_FUEL.get());
    }

    @Override
    protected void tick(ServerLevel level, Villager villager, long gameTime) {
        if (this.targetWorkstation == null) {
            return;
        }

        if (!villager.blockPosition().closerThan(this.targetWorkstation, CommonConfig.interactionDistance)) {
            return;
        }

        if (!this.atWorkstation) {
            handleArrival(level, villager, gameTime);
            return;
        }

        this.searchTicks++;

        if (this.searchTicks < SEARCH_DURATION) {
            int closeEnoughDistance = (int) (CommonConfig.interactionDistance / 2);
            villager.getBrain().setMemoryWithExpiry(MemoryModuleType.WALK_TARGET,
                new WalkTarget(this.targetWorkstation, 0.5F, closeEnoughDistance), 20L);

            villager.getLookControl().setLookAt(
                this.targetWorkstation.getX() + 0.5,
                this.targetWorkstation.getY() + 0.5,
                this.targetWorkstation.getZ() + 0.5
            );
            return;
        }

        placeFuel(level, villager);
        clearMemories(villager);
        stopBehavior(villager);
    }

    private void handleArrival(ServerLevel level, Villager villager, long gameTime) {
        this.atWorkstation = true;
        this.searchTicks = 0;

        if (!claimWorkstation(level, villager, gameTime)) {
            stopBehavior(villager);
            return;
        }

        ContainerHelper.openContainer(level, this.targetWorkstation);
    }

    @Override
    protected void stop(ServerLevel level, Villager villager, long gameTime) {
        villager.getBrain().eraseMemory(MemoryModuleTypes.BUSY.get());
        releaseWorkstation();
        clearMemories(villager);
        resetState();
    }

    private boolean claimWorkstation(ServerLevel level, Villager villager, long gameTime) {
        UUID villageId = villager.getBrain().getMemory(MemoryModuleTypes.VILLAGE.get()).orElse(null);
        if (villageId == null) {
            return false;
        }

        IVillageZone zone = findZoneContaining(level, villageId, this.targetWorkstation);
        if (zone == null) {
            return false;
        }

        boolean claimed = zone.claim(this.targetWorkstation, villager.getUUID(), CLAIM_DURATION, gameTime);
        if (!claimed) {
            VillageTale.LOGGER.debug("RefuelWorkstation failed to claim workstation at {} for villager {}",
                this.targetWorkstation, villager.getId());
            return false;
        }

        this.workstationClaimed = true;
        this.claimedZone = zone;

        VillageTale.LOGGER.debug("RefuelWorkstation claimed workstation at {} for villager {}",
            this.targetWorkstation, villager.getId());

        return true;
    }

    private void placeFuel(ServerLevel level, Villager villager) {
        Block block = level.getBlockState(this.targetWorkstation).getBlock();
        IRecipeManager recipeManager = RecipeManager.getInstance();

        int fuelSlot = recipeManager.getFuelSlotForBlock(block).orElse(-1);
        if (fuelSlot < 0) {
            return;
        }

        ItemOrTagMatcher fuelMatcher = recipeManager.getFuelItemsForBlock(block).orElse(null);
        if (fuelMatcher == null) {
            return;
        }

        SimpleContainer inventory = villager.getInventory();
        int placed = 0;

        for (int i = 0; i < inventory.getContainerSize() && placed < MAX_FUEL_TO_PLACE; i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }

            if (!fuelMatcher.matches(stack)) {
                continue;
            }

            int toPlace = Math.min(stack.getCount(), MAX_FUEL_TO_PLACE - placed);
            ItemStack fuelStack = stack.copy();
            fuelStack.setCount(toPlace);

            int actuallyPlaced = ContainerHelper.placeItemInSlot(level, this.targetWorkstation, fuelSlot, fuelStack);
            if (actuallyPlaced > 0) {
                stack.shrink(actuallyPlaced);
                placed += actuallyPlaced;

                VillageTale.LOGGER.debug("RefuelWorkstation placed {} x{} into slot {} for villager {}",
                    stack.getItem(), actuallyPlaced, fuelSlot, villager.getId());
            }
        }
    }

    private boolean hasFuel(Villager villager, ItemOrTagMatcher fuelMatcher) {
        for (int i = 0; i < villager.getInventory().getContainerSize(); i++) {
            ItemStack stack = villager.getInventory().getItem(i);
            if (!stack.isEmpty() && fuelMatcher.matches(stack)) {
                return true;
            }
        }
        return false;
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
                    Map<BlockPos, Optional<UUID>> claims = zone.getClaims(level.getGameTime());
                    return claims.containsKey(pos);
                })
                .findFirst()
                .orElse(null);
    }

    private void releaseWorkstation() {
        if (this.workstationClaimed && this.claimedZone != null && this.targetWorkstation != null) {
            boolean released = this.claimedZone.release(this.targetWorkstation);

            VillageTale.LOGGER.debug("RefuelWorkstation {} workstation at {}",
                released ? "released" : "failed to release", this.targetWorkstation);
        }
    }

    private void clearMemories(Villager villager) {
        villager.getBrain().eraseMemory(MemoryModuleTypes.WORKSTATION_NEEDS_FUEL.get());
    }

    private void resetState() {
        this.targetWorkstation = null;
        this.atWorkstation = false;
        this.workstationClaimed = false;
        this.searchTicks = 0;
        this.claimedZone = null;
    }

    private void stopBehavior(Villager villager) {
        villager.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
    }
}
