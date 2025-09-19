package org.sosly.villagetale.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import org.sosly.villagetale.VillageTale;
import org.sosly.villagetale.api.IVillageZone;
import org.sosly.villagetale.entity.MemoryModuleTypes;
import org.sosly.villagetale.entity.Villager;
import org.sosly.villagetale.helper.VillagesHelper;

public class GoToWorkZone extends Behavior<Villager> {
    private static final int BEHAVIOR_DURATION = 200;
    private static final double ARRIVAL_DISTANCE = 4.0;

    public GoToWorkZone() {
        super(ImmutableMap.of(
                MemoryModuleTypes.VILLAGE.get(), MemoryStatus.VALUE_PRESENT,
                MemoryModuleTypes.WORK_ZONE.get(), MemoryStatus.VALUE_PRESENT,
                MemoryModuleType.WALK_TARGET, MemoryStatus.VALUE_ABSENT
        ), BEHAVIOR_DURATION);
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, Villager villager) {
        UUID villageId = villager.getVillage().get();
        UUID workplaceId = villager.getBrain().getMemory(MemoryModuleTypes.WORK_ZONE.get()).orElse(null);
        if (workplaceId == null) {
            return false;
        }

        IVillageZone zone = VillagesHelper.getWorkZone(level, villager, villageId, workplaceId);
        if (zone == null) {
            return false;
        }

        return !zone.containsPosition(villager.blockPosition());
    }

    @Override
    protected void start(ServerLevel level, Villager villager, long gameTime) {
        UUID villageId = villager.getVillage().get();
        UUID workplaceId = villager.getBrain().getMemory(MemoryModuleTypes.WORK_ZONE.get()).orElse(null);
        if (workplaceId == null) {
            return;
        }

        IVillageZone zone = VillagesHelper.getWorkZone(level, villager, villageId, workplaceId);

        if (zone == null) {
            VillageTale.LOGGER.warn("No zone found for workplace " + workplaceId);
            return;
        }

        VillageTale.LOGGER.info(String.format("Going to work zone %s", workplaceId));
        villager.getBrain().setMemory(MemoryModuleType.WALK_TARGET,
            new WalkTarget(zone.getStartPosition(), 0.5F, 2));
    }

    @Override
    protected boolean canStillUse(ServerLevel level, Villager villager, long gameTime) {
        UUID villageId = villager.getVillage().get();
        UUID workplaceId = villager.getBrain().getMemory(MemoryModuleTypes.WORK_ZONE.get()).orElse(null);
        if (workplaceId == null) {
            return false;
        }

        IVillageZone zone = VillagesHelper.getWorkZone(level, villager, villageId, workplaceId);
        if (zone == null) {
            return false;
        }

        return !villager.blockPosition()
                .closerThan(zone.getStartPosition().atY(villager.getBlockY()), ARRIVAL_DISTANCE);
    }
}
