package org.sosly.villagetale.entity.ai.sensor;

import com.google.common.collect.ImmutableSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.sensing.Sensor;
import org.jetbrains.annotations.NotNull;
import org.sosly.villagetale.VillageTale;
import org.sosly.villagetale.api.IVillageZone;
import org.sosly.villagetale.entity.MemoryModuleTypes;
import org.sosly.villagetale.entity.Villager;
import org.sosly.villagetale.helper.VillagesHelper;

public class AreZoneMemoriesValid extends Sensor<Villager> {
    private static final int SCAN_RATE = 200;

    public AreZoneMemoriesValid() {
        super(SCAN_RATE);
    }

    @Override
    protected void doTick(ServerLevel level, Villager villager) {
        Optional<UUID> villageId = villager.getVillage();
        if (villageId.isEmpty()) {
            clearAllZoneMemories(villager);
            return;
        }

        validateWorkZone(level, villager, villageId.get());
        validateHomeZone(level, villager, villageId.get());
    }

    private void clearAllZoneMemories(Villager villager) {
        villager.getBrain().eraseMemory(MemoryModuleTypes.WORK_ZONE.get());
        villager.getBrain().eraseMemory(MemoryModuleTypes.HOME_ZONE.get());
        villager.getBrain().eraseMemory(MemoryModuleType.HOME);
    }

    private void validateWorkZone(ServerLevel level, Villager villager, UUID villageId) {
        UUID workZoneId = villager.getBrain().getMemory(MemoryModuleTypes.WORK_ZONE.get()).orElse(null);
        if (workZoneId == null) {
            return;
        }

        IVillageZone workZone = VillagesHelper.getZoneById(level, villageId, workZoneId);
        if (workZone != null) {
            return;
        }

        VillageTale.LOGGER.info("Clearing invalid work zone memory {} for villager {}",
                workZoneId, villager.getUUID());
        villager.getBrain().eraseMemory(MemoryModuleTypes.WORK_ZONE.get());
    }

    private void validateHomeZone(ServerLevel level, Villager villager, UUID villageId) {
        UUID homeZoneId = villager.getBrain().getMemory(MemoryModuleTypes.HOME_ZONE.get()).orElse(null);
        if (homeZoneId == null) {
            return;
        }

        IVillageZone homeZone = VillagesHelper.getZoneById(level, villageId, homeZoneId);
        if (homeZone != null) {
            return;
        }

        VillageTale.LOGGER.info("Clearing invalid home zone memory {} for villager {}",
                homeZoneId, villager.getUUID());
        villager.getBrain().eraseMemory(MemoryModuleTypes.HOME_ZONE.get());
        villager.getBrain().eraseMemory(MemoryModuleType.HOME);
    }

    @Override
    public @NotNull Set<MemoryModuleType<?>> requires() {
        return ImmutableSet.of(
            MemoryModuleTypes.VILLAGE.get(),
            MemoryModuleTypes.WORK_ZONE.get(),
            MemoryModuleTypes.HOME_ZONE.get(),
            MemoryModuleType.HOME
        );
    }
}
