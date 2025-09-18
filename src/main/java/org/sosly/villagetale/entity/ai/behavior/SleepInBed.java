package org.sosly.villagetale.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.sosly.villagetale.entity.Villager;

import java.util.Optional;

public class SleepInBed extends Behavior<Villager> {
    private long nextOkStartTime;

    public SleepInBed() {
        super(ImmutableMap.of(MemoryModuleType.HOME, MemoryStatus.VALUE_PRESENT, MemoryModuleType.LAST_WOKEN, MemoryStatus.REGISTERED));
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, Villager villager) {
        if (villager.isPassenger()) {
            return false;
        }

        GlobalPos homePos = villager.getBrain().getMemory(MemoryModuleType.HOME).get();
        if (level.dimension() != homePos.dimension()) {
            return false;
        }

        Optional<Long> lastWoken = villager.getBrain().getMemory(MemoryModuleType.LAST_WOKEN);
        if (lastWoken.isPresent()) {
            long timeSinceWoken = level.getGameTime() - lastWoken.get();
            if (timeSinceWoken > 0L && timeSinceWoken < 100L) {
                return false;
            }
        }

        BlockState blockState = level.getBlockState(homePos.pos());
        return homePos.pos().closerToCenterThan(villager.position(), 2.0D)
            && blockState.is(BlockTags.BEDS)
            && !blockState.getValue(BedBlock.OCCUPIED);
    }

    @Override
    protected boolean canStillUse(ServerLevel level, Villager villager, long gameTime) {
        Optional<GlobalPos> homePos = villager.getBrain().getMemory(MemoryModuleType.HOME);
        if (homePos.isEmpty()) {
            return false;
        }

        BlockPos bedPos = homePos.get().pos();
        return villager.getBrain().isActive(Activity.REST)
            && villager.getY() > (double) bedPos.getY() + 0.4D
            && bedPos.closerToCenterThan(villager.position(), 1.14D);
    }

    @Override
    protected void start(ServerLevel level, Villager villager, long gameTime) {
        if (gameTime > this.nextOkStartTime) {
            BlockPos bedPos = villager.getBrain().getMemory(MemoryModuleType.HOME).get().pos();
            villager.startSleeping(bedPos);
        }
    }

    @Override
    protected boolean timedOut(long gameTime) {
        return false;
    }

    @Override
    protected void stop(ServerLevel level, Villager villager, long gameTime) {
        if (villager.isSleeping()) {
            villager.stopSleeping();
            this.nextOkStartTime = gameTime + 40L;
        }
    }
}
