package org.sosly.villagetale.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.sosly.villagetale.entity.Villager;

import java.util.Optional;

public class SetWalkTargetFromBlockMemory extends Behavior<Villager> {
    private static final int MAX_WALK_ATTEMPTS = 1000;
    private static final int WALK_RADIUS = 15;
    private static final int WALK_HEIGHT = 7;
    private static final double WALK_ANGLE = Math.PI / 2F;
    
    private final MemoryModuleType<GlobalPos> targetMemoryType;
    private final float speedModifier;
    private final int closeEnoughDistance;
    private final int tooFarDistance;
    private final int maxUnreachableTime;

    public SetWalkTargetFromBlockMemory(
            MemoryModuleType<GlobalPos> targetMemoryType,
            float speedModifier,
            int closeEnoughDistance,
            int tooFarDistance,
            int maxUnreachableTime
    ) {
        super(ImmutableMap.of(
            targetMemoryType, MemoryStatus.VALUE_PRESENT,
            MemoryModuleType.WALK_TARGET, MemoryStatus.VALUE_ABSENT
        ), 20);
        
        this.targetMemoryType = targetMemoryType;
        this.speedModifier = speedModifier;
        this.closeEnoughDistance = closeEnoughDistance;
        this.tooFarDistance = tooFarDistance;
        this.maxUnreachableTime = maxUnreachableTime;
    }

    public static BehaviorControl<Villager> create(
            MemoryModuleType<GlobalPos> memoryType,
            float speedModifier,
            int closeEnoughDistance,
            int tooFarDistance,
            int maxUnreachableTime
    ) {
        return new SetWalkTargetFromBlockMemory(
            memoryType,
            speedModifier,
            closeEnoughDistance,
            tooFarDistance,
            maxUnreachableTime
        );
    }

    @Override
    protected boolean checkExtraStartConditions(@NotNull ServerLevel level, @NotNull Villager villager) {
        GlobalPos targetPos = villager.getBrain().getMemory(targetMemoryType).orElse(null);
        if (targetPos == null) {
            return false;
        }

        if (targetPos.dimension() != level.dimension()) {
            return false;
        }

        Optional<Long> cantReachTime = villager.getBrain().getMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
        if (cantReachTime.isPresent()) {
            long timeSinceCantReach = level.getGameTime() - cantReachTime.get();
            if (timeSinceCantReach > maxUnreachableTime) {
                return false;
            }
        }

        return true;
    }

    @Override
    protected void start(@NotNull ServerLevel level, @NotNull Villager villager, long gameTime) {
        GlobalPos targetPos = villager.getBrain().getMemory(targetMemoryType).orElse(null);
        if (targetPos == null) {
            return;
        }

        int distance = targetPos.pos().distManhattan(villager.blockPosition());

        if (distance > tooFarDistance) {
            Vec3 walkPos = findValidWalkPosition(villager, targetPos);
            if (walkPos != null) {
                villager.getBrain().setMemory(
                    MemoryModuleType.WALK_TARGET,
                    new WalkTarget(walkPos, speedModifier, closeEnoughDistance)
                );
            } else {
                villager.getBrain().eraseMemory(targetMemoryType);
                villager.getBrain().setMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE, gameTime);
            }
        } else if (distance > closeEnoughDistance) {
            villager.getBrain().setMemory(
                MemoryModuleType.WALK_TARGET,
                new WalkTarget(targetPos.pos(), speedModifier, closeEnoughDistance)
            );
        }
    }

    private Vec3 findValidWalkPosition(Villager villager, GlobalPos targetPos) {
        Vec3 targetVec = Vec3.atBottomCenterOf(targetPos.pos());

        for (int attempt = 0; attempt < MAX_WALK_ATTEMPTS; attempt++) {
            Vec3 walkPos = DefaultRandomPos.getPosTowards(
                villager,
                WALK_RADIUS,
                WALK_HEIGHT,
                targetVec,
                WALK_ANGLE
            );
            if (walkPos != null) {
                return walkPos;
            }
        }

        return null;
    }
}
