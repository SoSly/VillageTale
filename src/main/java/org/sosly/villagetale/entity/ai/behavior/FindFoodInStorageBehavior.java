package org.sosly.villagetale.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
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
import org.sosly.villagetale.api.capability.IVillageCapability;
import org.sosly.villagetale.api.capability.IVillagesCapability;
import org.sosly.villagetale.api.data.IVillageZone;
import org.sosly.villagetale.api.data.ZoneType;
import org.sosly.villagetale.capability.Capabilities;
import org.sosly.villagetale.data.VillageInfo;
import org.sosly.villagetale.entity.MemoryModuleTypes;
import org.sosly.villagetale.entity.Villager;
import org.sosly.villagetale.helper.ContainerHelper;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class FindFoodInStorageBehavior extends Behavior<Villager> {
    private static final int BEHAVIOR_DURATION = 100;
    private static final double INTERACTION_DISTANCE = 2.0D;
    private static final int SEARCH_DURATION = 20; // 1 second at 20 tps

    private BlockPos targetContainer;
    private boolean searchingForFood;
    private boolean atContainer;
    private int searchTicks;

    public FindFoodInStorageBehavior() {
        super(ImmutableMap.of(
            MemoryModuleTypes.IS_HUNGRY.get(), MemoryStatus.VALUE_PRESENT,
            MemoryModuleTypes.VILLAGE.get(), MemoryStatus.VALUE_PRESENT
        ), BEHAVIOR_DURATION);
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, Villager villager) {
        Boolean isHungry = villager.getBrain().getMemory(MemoryModuleTypes.IS_HUNGRY.get()).orElse(false);
        if (!isHungry) {
            return false;
        }

        if (hasAnyFood(villager)) {
            return false;
        }

        UUID villageId = villager.getBrain().getMemory(MemoryModuleTypes.VILLAGE.get()).orElse(null);
        if (villageId == null) {
            return false;
        }

        BlockPos nearestFood = findNearestStorageWithFood(level, villager, villageId);
        if (nearestFood == null) {
            return false;
        }

        this.targetContainer = nearestFood;
        return true;
    }

    @Override
    protected void start(ServerLevel level, Villager villager, long gameTime) {
        this.searchingForFood = true;
        this.atContainer = false;
        this.searchTicks = 0;
        villager.getBrain().setMemory(MemoryModuleType.WALK_TARGET,
            new WalkTarget(this.targetContainer, 0.5F, 1));
    }

    @Override
    protected boolean canStillUse(ServerLevel level, Villager villager, long gameTime) {
        if (!this.searchingForFood) {
            return false;
        }

        Boolean isHungry = villager.getBrain().getMemory(MemoryModuleTypes.IS_HUNGRY.get()).orElse(false);
        return isHungry && !hasAnyFood(villager);
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
            ContainerHelper.openContainer(level, this.targetContainer);
            villager.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
            return;
        }

        this.searchTicks++;

        if (this.searchTicks < SEARCH_DURATION) {
            return;
        }

        int emptySlot = findFirstEmptySlot(villager);
        if (emptySlot < 0) {
            this.searchingForFood = false;
            return;
        }

        ItemStack extractedFood = ContainerHelper.extractItemFromContainer(level, this.targetContainer, FindFoodInStorageBehavior::isFood);
        if (extractedFood.isEmpty()) {
            this.searchingForFood = false;
            return;
        }

        villager.getInventory().setItem(emptySlot, extractedFood);
        this.searchingForFood = false;
    }

    @Override
    protected void stop(ServerLevel level, Villager villager, long gameTime) {
        this.targetContainer = null;
        this.searchingForFood = false;
        this.atContainer = false;
        this.searchTicks = 0;
        villager.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
    }

    private BlockPos findNearestStorageWithFood(ServerLevel level, Villager villager, UUID villageId) {
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

        List<IVillageZone> zones = villageCapability.getZones();
        BlockPos villagerPos = villager.blockPosition();
        BlockPos nearestContainer = null;
        double nearestDistance = Double.MAX_VALUE;

        for (IVillageZone zone : zones) {
            if (zone.getType() != ZoneType.STORAGE) {
                continue;
            }

            Optional<List<BlockPos>> pois = zone.getPOIs();
            if (pois.isEmpty()) {
                continue;
            }

            for (BlockPos containerPos : pois.get()) {
                if (!ContainerHelper.hasMatchingItem(level, containerPos, FindFoodInStorageBehavior::isFood)) {
                    continue;
                }

                double distance = villagerPos.distSqr(containerPos);
                if (distance < nearestDistance) {
                    nearestDistance = distance;
                    nearestContainer = containerPos;
                }
            }
        }

        return nearestContainer;
    }

    private static boolean isFood(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        return stack.getFoodProperties(null) != null;
    }

    private boolean hasAnyFood(Villager villager) {
        SimpleContainer inventory = villager.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack item = inventory.getItem(i);
            if (!item.isEmpty() && item.isEdible()) {
                return true;
            }
        }
        return false;
    }

    private int findFirstEmptySlot(Villager villager) {
        SimpleContainer inventory = villager.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            if (inventory.getItem(i).isEmpty()) {
                return i;
            }
        }
        return -1;
    }
}
