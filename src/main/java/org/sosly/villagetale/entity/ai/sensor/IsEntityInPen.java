package org.sosly.villagetale.entity.ai.sensor;

import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import org.sosly.villagetale.VillageTale;
import org.sosly.villagetale.api.IVillageZone;
import org.sosly.villagetale.api.capability.IVillageCapability;
import org.sosly.villagetale.api.capability.IVillagesCapability;
import org.sosly.villagetale.capability.Capabilities;
import org.sosly.villagetale.config.CommonConfig;
import org.sosly.villagetale.data.VillageInfo;
import org.sosly.villagetale.entity.MemoryModuleTypes;
import org.sosly.villagetale.entity.Villager;
import org.sosly.villagetale.helper.InventoryHelper;
import org.sosly.villagetale.zone.type.Pen;

public class IsEntityInPen extends Sensor<Villager> {
    private static final int MINIMUM_ANIMALS_BEFORE_SLAUGHTER = 3;

    public IsEntityInPen() {
        super(20);
    }

    @Override
    protected void doTick(ServerLevel level, Villager villager) {
        if (InventoryHelper.getItem(villager, stack -> stack.is(Items.LEAD)).isEmpty()) {
            return;
        }

        if (villager.getBrain().hasMemoryValue(MemoryModuleTypes.FOUND_ENTITY.get())) {
            return;
        }

        UUID villageId = villager.getBrain().getMemory(MemoryModuleTypes.VILLAGE.get()).orElse(null);
        if (villageId == null) {
            return;
        }

        IVillageZone currentPenZone = getCurrentPenZone(level, villager, villageId);
        if (currentPenZone == null) {
            return;
        }

        List<UUID> alreadyScanned = villager.getBrain().getMemory(MemoryModuleTypes.ALREADY_SCANNED_PENS.get())
            .orElse(new ArrayList<>());

        if (alreadyScanned.contains(currentPenZone.getUUID())) {
            return;
        }

        scanPenZone(level, villager, currentPenZone, alreadyScanned);
    }

    private IVillageZone getCurrentPenZone(ServerLevel level, Villager villager, UUID villageId) {
        IVillagesCapability villagesCapability = level.getCapability(Capabilities.VILLAGES_CAPABILITY).orElse(null);
        if (villagesCapability == null) {
            return null;
        }

        VillageInfo village = villagesCapability.getVillageById(villageId);
        if (village == null) {
            return null;
        }

        BlockPos townHallPos = village.getTownHallPos();
        if (townHallPos == null) {
            return null;
        }

        ChunkPos townHallChunk = new ChunkPos(townHallPos);
        LevelChunk chunk = level.getChunk(townHallChunk.x, townHallChunk.z);

        IVillageCapability villageCapability = chunk.getCapability(Capabilities.VILLAGE_CAPABILITY).orElse(null);
        if (villageCapability == null) {
            return null;
        }

        BlockPos villagerPos = villager.blockPosition();
        return villageCapability.getZones()
                .stream()
                .filter(z -> z.getType().getID().equals(Pen.ID))
                .filter(z -> villagerPos.closerThan(z.getStartPosition().atY(villagerPos.getY()), CommonConfig.interactionDistance))
                .findFirst()
                .orElse(null);
    }

    private void scanPenZone(ServerLevel level, Villager villager, IVillageZone pen, List<UUID> alreadyScanned) {
        Set<ResourceLocation> entityTypeFilter = pen.getEntityTypeFilter();
        if (entityTypeFilter.isEmpty()) {
            addToScannedList(villager, pen.getUUID(), alreadyScanned);
            return;
        }

        List<Animal> animalsInPen = level.getEntitiesOfClass(Animal.class, villager.getBoundingBox().inflate(CommonConfig.scanRadius),
            animal -> animal.distanceToSqr(villager) <= CommonConfig.scanRadius * CommonConfig.scanRadius
                && pen.containsPosition(animal.blockPosition())
                && matchesFilter(animal.getType(), entityTypeFilter));

        Map<ResourceLocation, List<Animal>> animalsByType = new HashMap<>();
        for (Animal animal : animalsInPen) {
            ResourceLocation animalId = EntityType.getKey(animal.getType());
            animalsByType.computeIfAbsent(animalId, k -> new ArrayList<>()).add(animal);
        }

        for (Map.Entry<ResourceLocation, List<Animal>> entry : animalsByType.entrySet()) {
            List<Animal> animals = entry.getValue();
            if (animals.size() < MINIMUM_ANIMALS_BEFORE_SLAUGHTER) {
                continue;
            }

            Animal adultAnimal = animals.stream()
                .filter(a -> !a.isBaby())
                .findFirst()
                .orElse(null);

            if (adultAnimal != null) {
                villager.getBrain().setMemoryWithExpiry(MemoryModuleTypes.FOUND_ENTITY.get(), adultAnimal, 2400L);
                villager.getBrain().eraseMemory(MemoryModuleTypes.ALREADY_SCANNED_PENS.get());

                VillageTale.LOGGER.debug("IsEntityInPen found {} at {} for villager {}",
                    entry.getKey(), adultAnimal.blockPosition(), villager.getId());
                return;
            }
        }

        addToScannedList(villager, pen.getUUID(), alreadyScanned);
    }

    private void addToScannedList(Villager villager, UUID zoneId, List<UUID> alreadyScanned) {
        List<UUID> updatedList = new ArrayList<>(alreadyScanned);
        updatedList.add(zoneId);
        villager.getBrain().setMemoryWithExpiry(MemoryModuleTypes.ALREADY_SCANNED_PENS.get(), updatedList, 1000L);

        VillageTale.LOGGER.debug("IsEntityInPen added zone {} to scanned list for villager {}",
            zoneId, villager.getId());
    }

    private boolean matchesFilter(EntityType<?> entityType, Set<ResourceLocation> entityTypeFilter) {
        ResourceLocation entityId = EntityType.getKey(entityType);
        return entityTypeFilter.contains(entityId);
    }

    @Override
    public Set<MemoryModuleType<?>> requires() {
        return ImmutableSet.of(
            MemoryModuleTypes.FOUND_ENTITY.get(),
            MemoryModuleTypes.ALREADY_SCANNED_PENS.get(),
            MemoryModuleTypes.VILLAGE.get()
        );
    }
}
