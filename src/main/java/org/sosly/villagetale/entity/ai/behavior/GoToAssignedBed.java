package org.sosly.villagetale.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import org.sosly.villagetale.entity.Villager;

public class GoToAssignedBed extends Behavior<Villager> {
    private static final double ARRIVAL_DISTANCE = 2.0D;
    private static final int WALK_PRECISION = 1;
    private final float speedModifier;

    public GoToAssignedBed(float speedModifier) {
        super(ImmutableMap.of(
            MemoryModuleType.HOME, MemoryStatus.VALUE_PRESENT,
            MemoryModuleType.WALK_TARGET, MemoryStatus.VALUE_ABSENT
        ), 200);
        this.speedModifier = speedModifier;
    }

    public static BehaviorControl<Villager> create(float speedModifier) {
        return new GoToAssignedBed(speedModifier);
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, Villager villager) {
        GlobalPos homePos = villager.getBrain().getMemory(MemoryModuleType.HOME).orElse(null);
        if (homePos == null) {
            return false;
        }

        BlockPos bedPos = homePos.pos();
        return !bedPos.closerToCenterThan(villager.position(), ARRIVAL_DISTANCE);
    }

    @Override
    protected void start(ServerLevel level, Villager villager, long gameTime) {
        GlobalPos homePos = villager.getBrain().getMemory(MemoryModuleType.HOME).orElse(null);
        if (homePos == null) {
            return;
        }

        BlockPos bedPos = homePos.pos();
        villager.getBrain().setMemoryWithExpiry(MemoryModuleType.WALK_TARGET,
            new WalkTarget(bedPos, speedModifier, WALK_PRECISION), 200L);
    }

    @Override
    protected boolean canStillUse(ServerLevel level, Villager villager, long gameTime) {
        GlobalPos homePos = villager.getBrain().getMemory(MemoryModuleType.HOME).orElse(null);
        if (homePos == null) {
            return false;
        }
        
        BlockPos bedPos = homePos.pos();
        return !bedPos.closerToCenterThan(villager.position(), ARRIVAL_DISTANCE);
    }
    
    
    @Override
    protected void stop(ServerLevel level, Villager villager, long gameTime) {
        villager.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
    }
}
