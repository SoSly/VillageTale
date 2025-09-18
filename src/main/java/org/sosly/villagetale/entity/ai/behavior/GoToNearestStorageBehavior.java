package org.sosly.villagetale.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;
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

public class GoToNearestStorageBehavior extends Behavior<Villager> {
    private static final int BEHAVIOR_DURATION = 200;
    private static final double ARRIVAL_DISTANCE = 4.0D;

    private BlockPos targetStoragePos;

    public GoToNearestStorageBehavior() {
        super(ImmutableMap.of(
            MemoryModuleTypes.VILLAGE.get(), MemoryStatus.VALUE_PRESENT,
            MemoryModuleType.WALK_TARGET, MemoryStatus.VALUE_ABSENT
        ), BEHAVIOR_DURATION);
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, Villager villager) {
        if (villager.getBrain().hasMemoryValue(MemoryModuleTypes.FOUND_ITEM.get())) {
            return false;
        }

        boolean hasWantedItem = villager.getBrain().hasMemoryValue(MemoryModuleTypes.WANTED_ITEM.get());
        boolean hasItemsToDeposit = villager.getBrain().hasMemoryValue(MemoryModuleTypes.ITEMS_TO_DEPOSIT.get());

        if (!hasWantedItem && !hasItemsToDeposit) {
            return false;
        }

        UUID villageId = villager.getBrain().getMemory(MemoryModuleTypes.VILLAGE.get()).orElse(null);
        if (villageId == null) {
            return false;
        }

        boolean excludeScanned = hasWantedItem;
        BlockPos targetStorage = findNearestStorage(level, villager, villageId, excludeScanned);

        if (targetStorage == null && excludeScanned) {
            villager.getBrain().eraseMemory(MemoryModuleTypes.ALREADY_SCANNED_STORAGES.get());
            targetStorage = findNearestStorage(level, villager, villageId, false);
        }

        if (targetStorage == null) {
            return false;
        }

        if (villager.blockPosition().closerThan(targetStorage, ARRIVAL_DISTANCE)) {
            return false;
        }

        this.targetStoragePos = targetStorage;
        return true;
    }

    @Override
    protected void start(ServerLevel level, Villager villager, long gameTime) {
        if (this.targetStoragePos == null) {
            return;
        }

        villager.getBrain().setMemory(MemoryModuleType.WALK_TARGET,
            new WalkTarget(this.targetStoragePos, 0.5F, 2));

        if (VillageTale.LOGGER.isDebugEnabled()) {
            VillageTale.LOGGER.debug("GoToNearestStorage set walk target to {} for villager {}",
                this.targetStoragePos, villager.getId());
        }
    }

    @Override
    protected boolean canStillUse(ServerLevel level, Villager villager, long gameTime) {
        if (this.targetStoragePos == null) {
            return false;
        }

        boolean closeEnough = villager.blockPosition().closerThan(this.targetStoragePos, ARRIVAL_DISTANCE);
        if (closeEnough) {
            if (VillageTale.LOGGER.isDebugEnabled()) {
                VillageTale.LOGGER.debug("GoToNearestStorage stopping - villager {} arrived at storage {}",
                    villager.getId(), this.targetStoragePos);
            }
            return false;
        }

        return true;
    }

    private BlockPos findNearestStorage(ServerLevel level, Villager villager, UUID villageId, boolean excludeScanned) {
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
        BlockPos nearestZoneCenter = null;
        double nearestDistance = Double.MAX_VALUE;

        List<UUID> scannedStorages = excludeScanned ?
            villager.getBrain().getMemory(MemoryModuleTypes.ALREADY_SCANNED_STORAGES.get()).orElse(List.of()) :
            List.of();

        for (IVillageZone zone : zones) {
            if (zone.getType() != ZoneType.STORAGE) {
                continue;
            }

            if (excludeScanned && scannedStorages.contains(zone.getUUID())) {
                continue;
            }

            Optional<List<BlockPos>> pois = zone.getPOIs();
            if (pois.isEmpty()) {
                continue;
            }

            BlockPos zoneStart = zone.getStartPos();
            double distance = villagerPos.distSqr(zoneStart);

            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearestZoneCenter = zoneStart;
            }
        }

        return nearestZoneCenter;
    }
}
