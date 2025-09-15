package org.sosly.villageworks.entity.ai.behavior;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import org.sosly.villageworks.entity.Villager;

public class GoToAssignedBed {
    public static BehaviorControl<Villager> create(float speedModifier) {
        return BehaviorBuilder.create(instance -> 
            instance.group(
                instance.present(MemoryModuleType.HOME),
                instance.absent(MemoryModuleType.WALK_TARGET)
            ).apply(instance, (home, walkTarget) -> 
                (level, villager, gameTime) -> {
                    GlobalPos homePos = instance.get(home);
                    BlockPos bedPos = homePos.pos();
                    
                    if (!bedPos.closerToCenterThan(villager.position(), 2.0D)) {
                        walkTarget.set(new WalkTarget(bedPos, speedModifier, 1));
                        return true;
                    }
                    return false;
                }
            )
        );
    }
}