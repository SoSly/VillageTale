package org.sosly.villagetale.entity.ai.sensor;

import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.List;
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
import org.sosly.villagetale.api.data.IVillageZone;
import org.sosly.villagetale.api.data.ZoneType;
import org.sosly.villagetale.capability.Capabilities;
import org.sosly.villagetale.data.FoundItem;
import org.sosly.villagetale.data.VillageInfo;
import org.sosly.villagetale.data.WantedItem;
import org.sosly.villagetale.entity.MemoryModuleTypes;
import org.sosly.villagetale.entity.Villager;
import org.sosly.villagetale.helper.ContainerHelper;

public class IsItemInStorage extends Sensor<Villager> {
    private static final double ZONE_DETECTION_DISTANCE = 4.0D;

    public IsItemInStorage() {
        super(20);
    }

    @Override
    protected void doTick(ServerLevel level, Villager villager) {
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
            return;
        }

        List<UUID> alreadyScanned = villager.getBrain().getMemory(MemoryModuleTypes.ALREADY_SCANNED_STORAGES.get())
            .orElse(new ArrayList<>());

        if (alreadyScanned.contains(currentStorageZone.getUUID())) {
            return;
        }

        scanStorageZone(level, villager, currentStorageZone, wantedItem, alreadyScanned);
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
                .filter(z -> z.getType() == ZoneType.STORAGE)
                .filter(z -> villagerPos.closerThan(z.getStartPos(), ZONE_DETECTION_DISTANCE))
                .findFirst().orElse(null);
    }

    private void scanStorageZone(ServerLevel level, Villager villager, IVillageZone zone, WantedItem wantedItem, List<UUID> alreadyScanned) {
        Optional<List<BlockPos>> pois = zone.getPOIs();
        if (pois.isEmpty()) {
            addToScannedList(villager, zone.getUUID(), alreadyScanned);
            return;
        }

        for (BlockPos containerPos : pois.get()) {
            ResourceLocation itemId = ContainerHelper.getFirstMatchingItemId(level, containerPos, wantedItem.getMatcher());
            if (itemId == null) {
                continue;
            }

            FoundItem foundItem = new FoundItem(containerPos, itemId);
            villager.getBrain().setMemory(MemoryModuleTypes.FOUND_ITEM.get(), foundItem);

            if (VillageTale.LOGGER.isDebugEnabled()) {
                VillageTale.LOGGER.debug("IsItemInStorage found item {} at {} for villager {}",
                    itemId, containerPos, villager.getId());
            }
            return;
        }

        addToScannedList(villager, zone.getUUID(), alreadyScanned);
    }


    private void addToScannedList(Villager villager, UUID zoneId, List<UUID> alreadyScanned) {
        List<UUID> updatedList = new ArrayList<>(alreadyScanned);
        updatedList.add(zoneId);
        villager.getBrain().setMemory(MemoryModuleTypes.ALREADY_SCANNED_STORAGES.get(), updatedList);

        if (VillageTale.LOGGER.isDebugEnabled()) {
            VillageTale.LOGGER.debug("IsItemInStorage added zone {} to scanned list for villager {}",
                zoneId, villager.getId());
        }
    }

    @Override
    public Set<MemoryModuleType<?>> requires() {
        return ImmutableSet.of(
            MemoryModuleTypes.WANTED_ITEM.get(),
            MemoryModuleTypes.ALREADY_SCANNED_STORAGES.get(),
            MemoryModuleTypes.FOUND_ITEM.get(),
            MemoryModuleTypes.VILLAGE.get()
        );
    }
}
