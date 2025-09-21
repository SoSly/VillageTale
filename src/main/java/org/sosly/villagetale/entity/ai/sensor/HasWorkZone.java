package org.sosly.villagetale.entity.ai.sensor;

import com.google.common.collect.ImmutableSet;
import java.util.Optional;
import java.util.Set;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.sensing.Sensor;
import org.jetbrains.annotations.NotNull;
import org.sosly.villagetale.VillageTale;
import org.sosly.villagetale.api.IVillageZone;
import org.sosly.villagetale.api.capability.IVillageCapability;
import org.sosly.villagetale.entity.MemoryModuleTypes;
import org.sosly.villagetale.entity.Villager;
import org.sosly.villagetale.helper.VillagesHelper;
import org.sosly.villagetale.profession.professions.Commoner;

public class HasWorkZone extends Sensor<Villager> {
    public HasWorkZone() {
        super(200);
    }

    @Override
    protected void doTick(ServerLevel level, Villager villager) {
        if (villager.getVillage().isEmpty()) {
            return;
        }

        if (villager.getBrain().hasMemoryValue(MemoryModuleTypes.WORK_ZONE.get())) {
            return;
        }

        if (villager.getProfession().getID().equals(Commoner.ID)) {
            return;
        }

        IVillageCapability village = VillagesHelper.getVillageCapability(level, villager.getVillage().get());
        if (village == null) {
            return;
        }

        Optional<IVillageZone> zone = village.getZones()
            .stream()
            .filter(z -> z.getAssignedVillagers().contains(villager.getUUID()))
            .filter(z -> villager.getProfession().isValidWorkZone(z))
            .findFirst();

        if (zone.isEmpty()) {
            return;
        }

        villager.getBrain().setMemoryWithExpiry(MemoryModuleTypes.WORK_ZONE.get(), zone.get().getUUID(), 200);

        if (VillageTale.LOGGER.isDebugEnabled()) {
            VillageTale.LOGGER.debug("HasWorkZone: " + villager );
        }
    }

    @Override
    public @NotNull Set<MemoryModuleType<?>> requires() {
        return ImmutableSet.of(
            MemoryModuleTypes.WORK_ZONE.get()
        );
    }
}
