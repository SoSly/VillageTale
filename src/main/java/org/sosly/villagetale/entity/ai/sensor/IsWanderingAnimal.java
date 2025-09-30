package org.sosly.villagetale.entity.ai.sensor;

import com.google.common.collect.ImmutableSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.NotNull;
import org.sosly.villagetale.api.IVillageZone;
import org.sosly.villagetale.api.capability.IVillageCapability;
import org.sosly.villagetale.api.capability.IVillagesCapability;
import org.sosly.villagetale.capability.Capabilities;
import org.sosly.villagetale.data.VillageInfo;
import org.sosly.villagetale.entity.MemoryModuleTypes;
import org.sosly.villagetale.entity.Villager;
import org.sosly.villagetale.helper.VillagesHelper;
import org.sosly.villagetale.zone.type.Pen;

public class IsWanderingAnimal extends Sensor<Villager> {
    public IsWanderingAnimal() {
        super(200);
    }

    @Override
    protected void doTick(ServerLevel level, Villager villager) {
        if (villager.getVillage().isEmpty()) {
            return;
        }

        if (!villager.getBrain().hasMemoryValue(MemoryModuleTypes.WORK_ZONE.get())) {
            return;
        }

        IVillagesCapability villages = level.getCapability(Capabilities.VILLAGES_CAPABILITY).orElse(null);
        if (villages == null) {
            return;
        }

        VillageInfo villageInfo = villages.getVillageById(villager.getVillage().get());
        if (villageInfo == null) {
            return;
        }

        IVillageCapability village = VillagesHelper.getVillageCapability(level, villager.getVillage().get());
        if (village == null) {
            return;
        }

        Optional<IVillageZone> workZone = village.getZones()
            .stream()
            .filter(z -> z.getUUID().equals(villager.getBrain().getMemory(MemoryModuleTypes.WORK_ZONE.get()).orElse(null)))
            .filter(z -> z.getType().getID().equals(Pen.ID))
            .findFirst();

        if (workZone.isEmpty()) {
            return;
        }

        IVillageZone pen = workZone.get();
        Set<ResourceLocation> entityTypeFilter = pen.getEntityTypeFilter();

        if (entityTypeFilter.isEmpty()) {
            return;
        }

        ChunkPos centerChunk = villageInfo.getTownHallPos() != null
            ? new ChunkPos(villageInfo.getTownHallPos())
            : villageInfo.getVillageStartingChunk();
        int squadius = villageInfo.getSquadius();

        int minX = (centerChunk.x - squadius) << 4;
        int maxX = ((centerChunk.x + squadius) << 4) + 15;
        int minZ = (centerChunk.z - squadius) << 4;
        int maxZ = ((centerChunk.z + squadius) << 4) + 15;

        AABB villageArea = new AABB(minX, level.getMinBuildHeight(), minZ, maxX, level.getMaxBuildHeight(), maxZ);
        List<Animal> nearbyAnimals = level.getEntitiesOfClass(Animal.class, villageArea);

        for (Animal animal : nearbyAnimals) {
            if (pen.containsPosition(animal.blockPosition())) {
                continue;
            }

            EntityType<?> animalType = animal.getType();
            if (!matchesFilter(animalType, entityTypeFilter)) {
                continue;
            }

            villager.getBrain().setMemoryWithExpiry(MemoryModuleTypes.WANDERING_ANIMAL.get(), animal, 200);
            return;
        }

        villager.getBrain().eraseMemory(MemoryModuleTypes.WANDERING_ANIMAL.get());
    }

    private boolean matchesFilter(EntityType<?> entityType, Set<ResourceLocation> entityTypeFilter) {
        ResourceLocation entityId = EntityType.getKey(entityType);
        return entityTypeFilter.contains(entityId);
    }

    @Override
    public @NotNull Set<MemoryModuleType<?>> requires() {
        return ImmutableSet.of(
            MemoryModuleTypes.WORK_ZONE.get(),
            MemoryModuleTypes.WANDERING_ANIMAL.get()
        );
    }
}
