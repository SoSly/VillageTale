package org.sosly.villagetale.entity.ai.behavior;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraftforge.common.Tags;
import org.sosly.villagetale.entity.MemoryModuleTypes;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class CloseOpenedGates {
    private static final double MIN_DISTANCE_TO_CLOSE = 2.0D;
    private static final double MAX_DISTANCE_TO_KEEP_TRACKING = 5.0D;

    public static BehaviorControl<LivingEntity> create() {
        return BehaviorBuilder.create((instance) -> {
            return instance.group(
                    instance.present(MemoryModuleTypes.GATES_TO_CLOSE.get()),
                    instance.registered(MemoryModuleType.NEAREST_LIVING_ENTITIES),
                    instance.absent(MemoryModuleTypes.BUSY.get())
            ).apply(instance, (gatesToClose, nearestLivingEntities, busy) -> {
                return (level, entity, gameTime) -> {
                    Set<GlobalPos> gates = instance.get(gatesToClose);
                    
                    if (gates.isEmpty()) {
                        return false;
                    }
                    
                    Iterator<GlobalPos> iterator = gates.iterator();
                    while(iterator.hasNext()) {
                        GlobalPos globalpos = iterator.next();
                        BlockPos blockpos = globalpos.pos();
                        
                        double distance = Math.sqrt(blockpos.distSqr(entity.blockPosition()));
                        
                        if (distance < MIN_DISTANCE_TO_CLOSE) {
                            continue;
                        }
                        
                        if (distance > MAX_DISTANCE_TO_KEEP_TRACKING) {
                            iterator.remove();
                            continue;
                        }
                        
                        BlockState blockstate = level.getBlockState(blockpos);
                        if (!blockstate.is(Tags.Blocks.FENCE_GATES)) {
                            iterator.remove();
                        } else if (!blockstate.getValue(BlockStateProperties.OPEN)) {
                            iterator.remove();
                        } else if (areOtherMobsNearGate(entity, blockpos, instance.tryGet(nearestLivingEntities))) {
                            // Keep checking later
                        } else {
                            level.setBlock(blockpos, blockstate.setValue(BlockStateProperties.OPEN, false), 10);
                            iterator.remove();
                        }
                    }
                    
                    return true;
                };
            });
        });
    }
    
    private static boolean areOtherMobsNearGate(LivingEntity entity, BlockPos pos, Optional<List<LivingEntity>> nearestLivingEntities) {
        if (!nearestLivingEntities.isPresent()) {
            return false;
        }
        
        return nearestLivingEntities.get().stream()
                .filter((otherEntity) -> otherEntity != entity)
                .filter((otherEntity) -> {
                    if (otherEntity.getType() == entity.getType()) {
                        return true;
                    }
                    if (otherEntity instanceof Mob mob && mob.isLeashed() && mob.getLeashHolder() == entity) {
                        return true;
                    }
                    return false;
                })
                .anyMatch((otherEntity) -> pos.closerToCenterThan(otherEntity.position(), 2.0D));
    }
}