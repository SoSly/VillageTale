package org.sosly.villagetale.entity.ai.sensor;

import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import org.sosly.villagetale.VillageTale;
import org.sosly.villagetale.api.capability.IVillageCapability;
import org.sosly.villagetale.api.capability.IVillagesCapability;
import org.sosly.villagetale.api.IVillageZone;
import org.sosly.villagetale.config.CommonConfig;
import org.sosly.villagetale.zone.type.Storage;
import org.sosly.villagetale.capability.Capabilities;
import org.sosly.villagetale.data.FoundItem;
import org.sosly.villagetale.data.TimedWantedItem;
import org.sosly.villagetale.data.VillageInfo;
import org.sosly.villagetale.data.WantedItem;
import org.sosly.villagetale.entity.MemoryModuleTypes;
import org.sosly.villagetale.entity.Villager;
import org.sosly.villagetale.helper.ContainerHelper;

public class IsItemInStorage extends Sensor<Villager> {

    public IsItemInStorage() {
        super(20);
    }

    @Override
    protected void doTick(ServerLevel level, Villager villager) {
        cleanupExpiredCouldNotFind(level, villager);

        if (villager.getBrain().hasMemoryValue(MemoryModuleTypes.FOUND_ITEM.get())) {
            return;
        }

        WantedItem wantedItem = villager.getBrain().getMemory(MemoryModuleTypes.WANTED_ITEM.get()).orElse(null);
        if (wantedItem == null) {
            return;
        }

        UUID villageId = villager.getBrain().getMemory(MemoryModuleTypes.VILLAGE.get()).orElse(null);
        if (villageId == null) {
            return;
        }

        IVillageZone currentStorageZone = getCurrentStorageZone(level, villager, villageId);
        if (currentStorageZone == null) {
            checkIfAllStoragesScanned(level, villager, villageId, wantedItem);
            return;
        }

        List<UUID> alreadyScanned = villager.getBrain().getMemory(MemoryModuleTypes.ALREADY_SCANNED_STORAGES.get())
            .orElse(new ArrayList<>());

        if (alreadyScanned.contains(currentStorageZone.getUUID())) {
            checkIfAllStoragesScanned(level, villager, villageId, wantedItem);
            return;
        }

        scanStorageZone(level, villager, currentStorageZone, wantedItem, alreadyScanned);
        checkIfAllStoragesScanned(level, villager, villageId, wantedItem);
    }

    private IVillageZone getCurrentStorageZone(ServerLevel level, Villager villager, UUID villageId) {
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

        BlockPos villagerPos = villager.blockPosition();
        return villageCapability.getZones()
                .stream()
                .filter(z -> z.getType().getID().equals(Storage.ID))
                .filter(z -> villagerPos.closerThan(z.getStartPosition().atY(villagerPos.getY()), CommonConfig.interactionDistance))
                .findFirst()
                .orElse(null);
    }

    private void scanStorageZone(ServerLevel level, Villager villager, IVillageZone zone, WantedItem wantedItem, List<UUID> alreadyScanned) {
        Map<BlockPos, Optional<UUID>> claims = zone.getClaims(level.getGameTime());
        if (claims.isEmpty()) {
            addToScannedList(villager, zone.getUUID(), alreadyScanned);
            return;
        }

        addToScannedList(villager, zone.getUUID(), alreadyScanned);

        for (BlockPos containerPos : claims.keySet()) {
            ResourceLocation itemId = ContainerHelper.getFirstMatchingItemId(level, containerPos, wantedItem.getMatcher());
            if (itemId == null) {
                continue;
            }

            FoundItem foundItem = new FoundItem(containerPos, itemId);
            villager.getBrain().setMemory(MemoryModuleTypes.FOUND_ITEM.get(), foundItem);
            villager.getBrain().eraseMemory(MemoryModuleTypes.ALREADY_SCANNED_STORAGES.get());

            VillageTale.LOGGER.debug("IsItemInStorage found item {} at {} for villager {}",
                itemId, containerPos, villager.getId());
            return;
        }
    }


    private void addToScannedList(Villager villager, UUID zoneId, List<UUID> alreadyScanned) {
        List<UUID> updatedList = new ArrayList<>(alreadyScanned);
        updatedList.add(zoneId);
        villager.getBrain().setMemory(MemoryModuleTypes.ALREADY_SCANNED_STORAGES.get(), updatedList);

        VillageTale.LOGGER.debug("IsItemInStorage added zone {} to scanned list for villager {}",
            zoneId, villager.getId());
    }

    private void checkIfAllStoragesScanned(ServerLevel level, Villager villager, UUID villageId, WantedItem wantedItem) {
        if (villager.getBrain().hasMemoryValue(MemoryModuleTypes.FOUND_ITEM.get())) {
            return;
        }

        IVillagesCapability villagesCapability = level.getCapability(Capabilities.VILLAGES_CAPABILITY).orElse(null);
        if (villagesCapability == null) {
            return;
        }

        VillageInfo village = villagesCapability.getVillageById(villageId);
        if (village == null) {
            return;
        }

        ChunkPos townHallChunk = new ChunkPos(village.getTownHallPos());
        LevelChunk chunk = level.getChunk(townHallChunk.x, townHallChunk.z);

        IVillageCapability villageCapability = chunk.getCapability(Capabilities.VILLAGE_CAPABILITY).orElse(null);
        if (villageCapability == null) {
            return;
        }

        long totalStorageZones = villageCapability.getZones().stream()
            .filter(z -> z.getType().getID().equals(Storage.ID))
            .count();

        List<UUID> alreadyScanned = villager.getBrain().getMemory(MemoryModuleTypes.ALREADY_SCANNED_STORAGES.get())
            .orElse(new ArrayList<>());

        if (alreadyScanned.size() >= totalStorageZones && totalStorageZones > 0) {
            addToCouldNotFindList(level, villager, wantedItem);
            villager.getBrain().eraseMemory(MemoryModuleTypes.ALREADY_SCANNED_STORAGES.get());

            VillageTale.LOGGER.debug("IsItemInStorage checked all {} storage zones, could not find item for villager {}",
                totalStorageZones, villager.getId());
        }
    }

    private void addToCouldNotFindList(ServerLevel level, Villager villager, WantedItem wantedItem) {
        List<TimedWantedItem> couldNotFind = villager.getBrain().getMemory(MemoryModuleTypes.COULD_NOT_FIND_ITEM.get())
            .orElse(new ArrayList<>());

        List<TimedWantedItem> updatedList = new ArrayList<>(couldNotFind);
        TimedWantedItem timedItem = new TimedWantedItem(wantedItem, level.getGameTime() + 18000L);
        updatedList.add(timedItem);

        villager.getBrain().setMemory(MemoryModuleTypes.COULD_NOT_FIND_ITEM.get(), updatedList);
    }

    private void cleanupExpiredCouldNotFind(ServerLevel level, Villager villager) {
        List<TimedWantedItem> couldNotFind = villager.getBrain().getMemory(MemoryModuleTypes.COULD_NOT_FIND_ITEM.get())
            .orElse(null);

        if (couldNotFind == null || couldNotFind.isEmpty()) {
            return;
        }

        long currentTime = level.getGameTime();
        List<TimedWantedItem> filtered = couldNotFind.stream()
            .filter(item -> !item.isExpired(currentTime))
            .toList();

        if (filtered.size() >= couldNotFind.size()) {
            return;
        }

        if (filtered.isEmpty()) {
            villager.getBrain().eraseMemory(MemoryModuleTypes.COULD_NOT_FIND_ITEM.get());
        } else {
            villager.getBrain().setMemory(MemoryModuleTypes.COULD_NOT_FIND_ITEM.get(), filtered);
        }
    }

    @Override
    public Set<MemoryModuleType<?>> requires() {
        return ImmutableSet.of(
            MemoryModuleTypes.WANTED_ITEM.get(),
            MemoryModuleTypes.ALREADY_SCANNED_STORAGES.get(),
            MemoryModuleTypes.FOUND_ITEM.get(),
            MemoryModuleTypes.VILLAGE.get(),
            MemoryModuleTypes.COULD_NOT_FIND_ITEM.get()
        );
    }
}
