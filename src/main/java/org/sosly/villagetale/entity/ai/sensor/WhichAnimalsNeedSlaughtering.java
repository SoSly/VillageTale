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
import org.jetbrains.annotations.NotNull;
import org.sosly.villagetale.VillageTale;
import org.sosly.villagetale.api.IVillageZone;
import org.sosly.villagetale.api.capability.IVillageCapability;
import org.sosly.villagetale.config.CommonConfig;
import org.sosly.villagetale.entity.MemoryModuleTypes;
import org.sosly.villagetale.entity.Villager;
import org.sosly.villagetale.helper.VillagesHelper;
import org.sosly.villagetale.zone.type.Butchery;

public class WhichAnimalsNeedSlaughtering extends Sensor<Villager> {

    public WhichAnimalsNeedSlaughtering() {
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

        IVillageCapability village = VillagesHelper.getVillageCapability(level, villager.getVillage().get());
        if (village == null) {
            return;
        }

        Optional<IVillageZone> workZone = village.getZones()
            .stream()
            .filter(z -> z.getUUID().equals(villager.getBrain().getMemory(MemoryModuleTypes.WORK_ZONE.get()).orElse(null)))
            .filter(z -> z.getType().getID().equals(Butchery.ID))
            .findFirst();

        if (workZone.isEmpty()) {
            return;
        }

        IVillageZone butchery = workZone.get();
        Set<ResourceLocation> entityTypeFilter = butchery.getEntityTypeFilter();

        if (entityTypeFilter.isEmpty()) {
            return;
        }

        List<Animal> nearbyAnimals = level.getEntitiesOfClass(Animal.class, villager.getBoundingBox().inflate(CommonConfig.scanRadius),
            animal -> animal.distanceToSqr(villager) <= CommonConfig.scanRadius * CommonConfig.scanRadius
                && butchery.containsPosition(animal.blockPosition())
                && matchesFilter(animal.getType(), entityTypeFilter));

        villager.getBrain().eraseMemory(MemoryModuleTypes.SLAUGHTERABLE_ENTITY.get());

        for (Animal animal : nearbyAnimals) {
            if (!animal.isBaby()) {
                villager.getBrain().setMemoryWithExpiry(MemoryModuleTypes.SLAUGHTERABLE_ENTITY.get(), animal, 200);
                VillageTale.LOGGER.debug("Found slaughterable animal: " + animal);
                return;
            }
        }
    }

    private boolean matchesFilter(EntityType<?> entityType, Set<ResourceLocation> entityTypeFilter) {
        ResourceLocation entityId = EntityType.getKey(entityType);
        return entityTypeFilter.contains(entityId);
    }

    @Override
    public @NotNull Set<MemoryModuleType<?>> requires() {
        return ImmutableSet.of(
            MemoryModuleTypes.WORK_ZONE.get(),
            MemoryModuleTypes.SLAUGHTERABLE_ENTITY.get()
        );
    }
}
