package org.sosly.villagetale.entity.ai.behavior;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.behavior.SleepInBed;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.sosly.villagetale.VillageTale;
import org.sosly.villagetale.entity.Villager;

import java.util.Optional;

public class SleepInAssignedBed {

    public static BehaviorControl<Villager> create() {
        return BehaviorBuilder.create(instance -> {
            SleepInBed vanillaBehavior = new SleepInBed();

            return instance.group(instance.absent(null))
                .apply(instance, (memoryAccessor) -> (level, villager, gameTime) -> {
                    VillageTale.LOGGER.debug("SleepInAssignedBed: Checking conditions for villager {} at position {}",
                        villager.getId(), villager.blockPosition());

                    // Check passenger
                    if (villager.isPassenger()) {
                        VillageTale.LOGGER.debug("  FAILED: Villager is a passenger");
                        return false;
                    }

                    // Check HOME memory
                    Optional<GlobalPos> homeOpt = villager.getBrain().getMemory(MemoryModuleType.HOME);
                    if (!homeOpt.isPresent()) {
                        VillageTale.LOGGER.debug("  FAILED: No HOME memory");
                        return false;
                    }

                    GlobalPos home = homeOpt.get();
                    BlockPos bedPos = home.pos();

                    // Check dimension
                    if (level.dimension() != home.dimension()) {
                        VillageTale.LOGGER.debug("  FAILED: Wrong dimension");
                        return false;
                    }

                    // Check LAST_WOKEN
                    Optional<Long> lastWoken = villager.getBrain().getMemory(MemoryModuleType.LAST_WOKEN);
                    if (lastWoken.isPresent()) {
                        long timeSinceWoken = level.getGameTime() - lastWoken.get();
                        if (timeSinceWoken > 0L && timeSinceWoken < 100L) {
                            VillageTale.LOGGER.debug("  FAILED: Woken too recently ({} ticks ago)", timeSinceWoken);
                            return false;
                        }
                    }

                    // Check distance
                    double distance = bedPos.getCenter().distanceTo(villager.position());
                    if (distance > 2.0D) {
                        VillageTale.LOGGER.debug("  FAILED: Too far from bed ({} blocks)", distance);
                        return false;
                    }

                    // Check block state
                    BlockState bedState = level.getBlockState(bedPos);
                    if (!bedState.is(BlockTags.BEDS)) {
                        VillageTale.LOGGER.debug("  FAILED: Block at {} is not tagged as bed ({})",
                            bedPos, bedState.getBlock());
                        return false;
                    }

                    // Check occupied
                    if (bedState.getValue(BedBlock.OCCUPIED)) {
                        VillageTale.LOGGER.debug("  FAILED: Bed is marked as occupied");
                        return false;
                    }

                    VillageTale.LOGGER.debug("  All checks passed, calling vanilla SleepInBed");
                    boolean result = vanillaBehavior.tryStart(level, villager, gameTime);

                    if (!result) {
                        VillageTale.LOGGER.debug("  FAILED: Vanilla SleepInBed.tryStart returned false");
                    } else {
                        VillageTale.LOGGER.debug("  SUCCESS: Villager should be sleeping");
                    }

                    return result;
                });
        });
    }
}

