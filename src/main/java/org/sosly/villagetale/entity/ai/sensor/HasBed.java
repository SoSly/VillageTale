package org.sosly.villagetale.entity.ai.sensor;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import org.jetbrains.annotations.NotNull;
import org.sosly.villagetale.VillageTale;
import org.sosly.villagetale.api.IVillageZone;
import org.sosly.villagetale.entity.MemoryModuleTypes;
import org.sosly.villagetale.entity.Villager;
import org.sosly.villagetale.helper.VillagesHelper;

public class HasBed extends Sensor<Villager> {
    private static final int CLAIM_DURATION = 24000;

    public HasBed() {
        super(200);
    }

    @Override
    protected void doTick(ServerLevel level, Villager villager) {
        if (villager.getBrain().hasMemoryValue(MemoryModuleType.HOME)) {
            return;
        }

        Optional<UUID> homeZoneId = villager.getBrain().getMemory(MemoryModuleTypes.HOME_ZONE.get());
        Optional<UUID> villageId = villager.getBrain().getMemory(MemoryModuleTypes.VILLAGE.get());

        if (homeZoneId.isEmpty() || villageId.isEmpty()) {
            return;
        }

        IVillageZone zone = VillagesHelper.getHomeZone(level, villager);
        if (zone == null) {
            return;
        }

        List<BlockPos> availableBeds = zone.getAvailableClaims(level.getGameTime(), Optional.of(HasBed::isBedHead));
        if (availableBeds.isEmpty()) {
            return;
        }

        BlockPos nearest = findNearestBed(availableBeds, villager.blockPosition());
        if (nearest == null) {
            return;
        }

        if (!zone.claim(nearest, villager.getUUID(), CLAIM_DURATION, level.getGameTime())) {
            return;
        }

        villager.getBrain().setMemoryWithExpiry(MemoryModuleType.HOME, GlobalPos.of(level.dimension(), nearest), 24000L);
        VillageTale.LOGGER.info("Villager {} claimed bed at {} in home zone {}",
                villager.getUUID(), nearest, zone.getName());
    }

    @Override
    public @NotNull Set<MemoryModuleType<?>> requires() {
        return Set.of(
            MemoryModuleTypes.HOME_ZONE.get(),
            MemoryModuleTypes.VILLAGE.get()
        );
    }

    private BlockPos findNearestBed(List<BlockPos> beds, BlockPos villagerPos) {
        BlockPos nearest = null;
        double nearestDistance = Double.MAX_VALUE;

        for (BlockPos bed : beds) {
            double distance = bed.distSqr(villagerPos);
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearest = bed;
            }
        }

        return nearest;
    }

    private static boolean isBedHead(BlockState state) {
        if (!(state.getBlock() instanceof BedBlock)) {
            return false;
        }

        BedPart part = state.getValue(BedBlock.PART);
        return part == BedPart.HEAD;
    }

}
