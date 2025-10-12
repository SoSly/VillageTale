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
import net.minecraft.world.item.Items;
import org.sosly.villagetale.VillageTale;
import org.sosly.villagetale.api.IVillageZone;
import org.sosly.villagetale.api.capability.IVillageCapability;
import org.sosly.villagetale.config.CommonConfig;
import org.sosly.villagetale.entity.MemoryModuleTypes;
import org.sosly.villagetale.entity.Villager;
import org.sosly.villagetale.helper.InventoryHelper;
import org.sosly.villagetale.helper.VillagesHelper;
import org.sosly.villagetale.zone.type.Pen;

public class GoToNearestPen extends Behavior<Villager> {
    private static final int BEHAVIOR_DURATION = 200;
    private BlockPos targetPenPos;

    public GoToNearestPen() {
        super(ImmutableMap.of(
            MemoryModuleTypes.VILLAGE.get(), MemoryStatus.VALUE_PRESENT,
            MemoryModuleType.WALK_TARGET, MemoryStatus.VALUE_ABSENT
        ), BEHAVIOR_DURATION);
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, Villager villager) {
        if (InventoryHelper.getItem(villager, stack -> stack.is(Items.LEAD)).isEmpty()) {
            return false;
        }

        if (villager.getBrain().hasMemoryValue(MemoryModuleTypes.FOUND_ENTITY.get())) {
            return false;
        }

        UUID villageId = villager.getBrain().getMemory(MemoryModuleTypes.VILLAGE.get()).orElse(null);
        if (villageId == null) {
            return false;
        }

        BlockPos targetPen = findNearestPen(level, villager, villageId, true);

        if (targetPen == null) {
            villager.getBrain().eraseMemory(MemoryModuleTypes.ALREADY_SCANNED_PENS.get());
            targetPen = findNearestPen(level, villager, villageId, false);
        }

        if (targetPen == null) {
            return false;
        }

        if (villager.blockPosition().closerThan(targetPen, CommonConfig.interactionDistance)) {
            return false;
        }

        this.targetPenPos = targetPen;
        return true;
    }

    @Override
    protected void start(ServerLevel level, Villager villager, long gameTime) {
        if (this.targetPenPos == null) {
            return;
        }

        villager.getBrain().setMemoryWithExpiry(MemoryModuleType.WALK_TARGET,
            new WalkTarget(this.targetPenPos, 0.5F, 2), 200L);

        VillageTale.LOGGER.debug("GoToNearestPen set walk target to {} for villager {}",
            this.targetPenPos, villager.getId());
    }

    @Override
    protected boolean canStillUse(ServerLevel level, Villager villager, long gameTime) {
        if (this.targetPenPos == null) {
            return false;
        }

        return !villager.blockPosition()
                .closerThan(this.targetPenPos, CommonConfig.interactionDistance);
    }

    private BlockPos findNearestPen(ServerLevel level, Villager villager, UUID villageId, boolean excludeScanned) {
        IVillageCapability village = VillagesHelper.getVillageCapability(level, villageId);
        if (village == null) {
            return null;
        }

        List<IVillageZone> zones = village.getZones();
        BlockPos villagerPos = villager.blockPosition();
        BlockPos nearestZoneCenter = null;
        double nearestDistance = Double.MAX_VALUE;

        List<UUID> scannedPens = excludeScanned ?
            villager.getBrain().getMemory(MemoryModuleTypes.ALREADY_SCANNED_PENS.get()).orElse(List.of()) :
            List.of();

        for (IVillageZone zone : zones) {
            if (!zone.getType().getID().equals(Pen.ID)) {
                continue;
            }

            if (excludeScanned && scannedPens.contains(zone.getUUID())) {
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
}
