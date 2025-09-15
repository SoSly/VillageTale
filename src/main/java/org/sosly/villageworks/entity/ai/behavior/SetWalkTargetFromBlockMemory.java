package org.sosly.villageworks.entity.ai.behavior;

import com.mojang.datafixers.kinds.Const;
import com.mojang.datafixers.kinds.IdF;
import com.mojang.datafixers.kinds.OptionalBox;
import com.mojang.datafixers.util.Unit;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.OneShot;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.behavior.declarative.MemoryAccessor;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.phys.Vec3;
import org.sosly.villageworks.entity.Villager;

import java.util.Optional;

public class SetWalkTargetFromBlockMemory {

    public static OneShot<Villager> create(MemoryModuleType<GlobalPos> memoryType, float speedModifier, int closeEnoughDistance, int tooFarDistance, int maxUnreachableTime) {
        return BehaviorBuilder.create(instance ->
            instance.group(
                instance.registered(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE),
                instance.absent(MemoryModuleType.WALK_TARGET),
                instance.present(memoryType)
            ).apply(instance, (cantReachMemory, walkTargetMemory, blockPosMemory) ->
                (level, villager, gameTime) -> {
                    GlobalPos targetPos = instance.get(blockPosMemory);

                    if (!isValidTarget(level, villager, targetPos, instance, cantReachMemory, gameTime, maxUnreachableTime)) {
                        blockPosMemory.erase();
                        cantReachMemory.set(gameTime);
                        return true;
                    }

                    int distance = targetPos.pos().distManhattan(villager.blockPosition());

                    if (distance > tooFarDistance) {
                        setRandomWalkTarget(villager, targetPos, speedModifier, closeEnoughDistance, walkTargetMemory, blockPosMemory, cantReachMemory, gameTime);
                    } else if (distance > closeEnoughDistance) {
                        walkTargetMemory.set(new WalkTarget(targetPos.pos(), speedModifier, closeEnoughDistance));
                    }

                    return true;
                }
            )
        );
    }

    private static boolean isValidTarget(ServerLevel level, Villager villager, GlobalPos targetPos,
                                       BehaviorBuilder.Instance<Villager> instance,
                                       MemoryAccessor<OptionalBox.Mu, Long> cantReachMemory,
                                       long gameTime, int maxUnreachableTime) {
        if (targetPos.dimension() != level.dimension()) {
            return false;
        }

        Optional<Long> cantReachTime = instance.tryGet(cantReachMemory);
        return cantReachTime.isEmpty() || gameTime - cantReachTime.get() <= maxUnreachableTime;
    }

    private static void setRandomWalkTarget(Villager villager, GlobalPos targetPos, float speedModifier, int closeEnoughDistance,
                                          MemoryAccessor<Const.Mu<Unit>, WalkTarget> walkTargetMemory, MemoryAccessor<IdF.Mu, GlobalPos> blockPosMemory,
                                          MemoryAccessor<OptionalBox.Mu, Long> cantReachMemory, long gameTime) {
        Vec3 walkPos = findValidWalkPosition(villager, targetPos);

        if (walkPos != null) {
            walkTargetMemory.set(new WalkTarget(walkPos, speedModifier, closeEnoughDistance));
        } else {
            blockPosMemory.erase();
            cantReachMemory.set(gameTime);
        }
    }

    private static Vec3 findValidWalkPosition(Villager villager, GlobalPos targetPos) {
        Vec3 targetVec = Vec3.atBottomCenterOf(targetPos.pos());

        for (int attempt = 0; attempt < 1000; attempt++) {
            Vec3 walkPos = DefaultRandomPos.getPosTowards(villager, 15, 7, targetVec, Math.PI / 2F);
            if (walkPos != null) {
                return walkPos;
            }
        }

        return null;
    }
}
