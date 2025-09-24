package org.sosly.villagetale.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import org.sosly.villagetale.VillageTale;
import org.sosly.villagetale.api.IVillageZone;
import org.sosly.villagetale.api.IWantedItem;
import org.sosly.villagetale.api.capability.IVillageCapability;
import org.sosly.villagetale.config.CommonConfig;
import org.sosly.villagetale.data.TimedWantedItem;
import org.sosly.villagetale.entity.MemoryModuleTypes;
import org.sosly.villagetale.entity.Villager;
import org.sosly.villagetale.helper.VillagesHelper;
import org.sosly.villagetale.zone.type.Storage;

public class GoToNearestStorage extends Behavior<Villager> {
    private static final int BEHAVIOR_DURATION = 200;
    private BlockPos targetStoragePos;

    public GoToNearestStorage() {
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

        if (hasWantedItem) {
            IWantedItem wantedItem = villager.getBrain().getMemory(MemoryModuleTypes.WANTED_ITEM.get()).orElse(null);
            if (wantedItem != null && isInCouldNotFindList(level, villager, wantedItem)) {
                return false;
            }
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

        if (villager.blockPosition().closerThan(targetStorage, CommonConfig.interactionDistance)) {
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

        villager.getBrain().setMemoryWithExpiry(MemoryModuleType.WALK_TARGET,
            new WalkTarget(this.targetStoragePos, 0.5F, 2), 200L);

        VillageTale.LOGGER.debug("GoToNearestStorage set walk target to {} for villager {}",
            this.targetStoragePos, villager.getId());
    }

    @Override
    protected boolean canStillUse(ServerLevel level, Villager villager, long gameTime) {
        if (this.targetStoragePos == null) {
            return false;
        }

        return !villager.blockPosition()
                .closerThan(this.targetStoragePos, CommonConfig.interactionDistance);
    }

    private BlockPos findNearestStorage(ServerLevel level, Villager villager, UUID villageId, boolean excludeScanned) {
        IVillageCapability village = VillagesHelper.getVillageCapability(level, villageId);
        if (village == null) {
            return null;
        }

        List<IVillageZone> zones = village.getZones();
        BlockPos villagerPos = villager.blockPosition();
        BlockPos nearestZoneCenter = null;
        double nearestDistance = Double.MAX_VALUE;

        List<UUID> scannedStorages = excludeScanned ?
            villager.getBrain().getMemory(MemoryModuleTypes.ALREADY_SCANNED_STORAGES.get()).orElse(List.of()) :
            List.of();

        for (IVillageZone zone : zones) {
            if (!zone.getType().getID().equals(Storage.ID)) {
                continue;
            }

            if (excludeScanned && scannedStorages.contains(zone.getUUID())) {
                continue;
            }

            Map<BlockPos, Optional<UUID>> claims = zone.getClaims(level.getGameTime());
            if (claims.isEmpty()) {
                continue;
            }

            BlockPos zoneStart = zone.getStartPosition();
            double distance = villagerPos.distSqr(zoneStart);

            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearestZoneCenter = zoneStart;
            }
        }

        return nearestZoneCenter;
    }

    private boolean isInCouldNotFindList(ServerLevel level, Villager villager, IWantedItem wantedItem) {
        List<TimedWantedItem> couldNotFind = villager.getBrain().getMemory(MemoryModuleTypes.COULD_NOT_FIND_ITEM.get())
            .orElse(null);

        if (couldNotFind == null || couldNotFind.isEmpty()) {
            return false;
        }

        long currentTime = level.getGameTime();
        return couldNotFind.stream()
            .filter(item -> !item.isExpired(currentTime))
            .anyMatch(item -> item.matches(wantedItem));
    }
}
