package org.sosly.villagetale.entity.ai.behavior;

import com.google.common.collect.Sets;
import com.mojang.datafixers.kinds.OptionalBox;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.behavior.declarative.MemoryAccessor;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraftforge.common.Tags;
import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.commons.lang3.mutable.MutableObject;
import org.sosly.villagetale.entity.MemoryModuleTypes;

import javax.annotation.Nullable;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public class InteractWithGate {
    private static final int COOLDOWN_BEFORE_RERUNNING_IN_SAME_NODE = 20;
    private static final double SKIP_CLOSING_GATE_IF_FURTHER_AWAY_THAN = 3.0D;
    private static final double MAX_DISTANCE_TO_HOLD_GATE_OPEN_FOR_OTHER_MOBS = 2.0D;

    public static BehaviorControl<LivingEntity> create() {
        MutableObject<Node> mutableobject = new MutableObject<>(null);
        MutableInt mutableint = new MutableInt(0);
        return BehaviorBuilder.create((instance) -> {
            return instance.group(
                    instance.present(MemoryModuleType.PATH),
                    instance.registered(MemoryModuleTypes.GATES_TO_CLOSE.get()),
                    instance.registered(MemoryModuleType.NEAREST_LIVING_ENTITIES)
            ).apply(instance, (path, gatesToClose, nearestLivingEntities) -> {
                return (level, entity, gameTime) -> {
                    Path pathValue = instance.get(path);
                    
                    // Get the gates memory, which might not exist yet
                    Optional<Set<GlobalPos>> optional = instance.tryGet(gatesToClose);
                    
                    if (!pathValue.notStarted() && !pathValue.isDone()) {
                        if (Objects.equals(mutableobject.getValue(), pathValue.getNextNode())) {
                            mutableint.setValue(20);
                        } else if (mutableint.decrementAndGet() > 0) {
                            return false;
                        }

                        mutableobject.setValue(pathValue.getNextNode());
                        Node previousNode = pathValue.getPreviousNode();
                        Node nextNode = pathValue.getNextNode();
                        
                        BlockPos previousPos = previousNode.asBlockPos();
                        BlockState previousState = level.getBlockState(previousPos);
                        if (previousState.is(Tags.Blocks.FENCE_GATES)) {
                            if (!previousState.getValue(BlockStateProperties.OPEN)) {
                                level.setBlock(previousPos, previousState.setValue(BlockStateProperties.OPEN, true), 10);
                                optional = rememberGateToClose(gatesToClose, optional, level, previousPos);
                            }
                        }

                        BlockPos nextPos = nextNode.asBlockPos();
                        BlockState nextState = level.getBlockState(nextPos);
                        if (nextState.is(Tags.Blocks.FENCE_GATES)) {
                            if (!nextState.getValue(BlockStateProperties.OPEN)) {
                                level.setBlock(nextPos, nextState.setValue(BlockStateProperties.OPEN, true), 10);
                                optional = rememberGateToClose(gatesToClose, optional, level, nextPos);
                            }
                        }

                        optional.ifPresent((gatesSet) -> {
                            closeGatesThatIHaveOpenedOrPassedThrough(level, entity, previousNode, nextNode, gatesSet, instance.tryGet(nearestLivingEntities));
                        });
                        
                        return true;
                    } else {
                        return false;
                    }
                };
            });
        });
    }

    public static void closeGatesThatIHaveOpenedOrPassedThrough(ServerLevel level, LivingEntity entity, @Nullable Node previous, @Nullable Node next, Set<GlobalPos> gatePositions, Optional<List<LivingEntity>> nearestLivingEntities) {
        Iterator<GlobalPos> iterator = gatePositions.iterator();

        while(iterator.hasNext()) {
            GlobalPos globalpos = iterator.next();
            BlockPos blockpos = globalpos.pos();
            
            // Don't close if we're still too close to the gate (within 1.5 blocks)
            double distanceToGate = Math.sqrt(blockpos.distSqr(entity.blockPosition()));
            
            if (distanceToGate < 1.5) {
                continue;
            }
            
            if (isGateTooFarAway(level, entity, globalpos)) {
                iterator.remove();
            } else {
                BlockState blockstate = level.getBlockState(blockpos);
                if (!blockstate.is(Tags.Blocks.FENCE_GATES)) {
                    iterator.remove();
                } else {
                    if (!blockstate.getValue(BlockStateProperties.OPEN)) {
                        iterator.remove();
                    } else if (areOtherMobsComingThroughGate(entity, blockpos, nearestLivingEntities)) {
                        // Don't remove - keep checking later
                    } else {
                        level.setBlock(blockpos, blockstate.setValue(BlockStateProperties.OPEN, false), 10);
                        iterator.remove();
                    }
                }
            }
        }
    }

    private static boolean areOtherMobsComingThroughGate(LivingEntity entity, BlockPos pos, Optional<List<LivingEntity>> nearestLivingEntities) {
        return nearestLivingEntities.isEmpty() ? false : nearestLivingEntities.get().stream()
                .filter((otherEntity) -> otherEntity.getType() == entity.getType())
                .filter((otherEntity) -> pos.closerToCenterThan(otherEntity.position(), MAX_DISTANCE_TO_HOLD_GATE_OPEN_FOR_OTHER_MOBS))
                .anyMatch((otherEntity) -> isMobComingThroughGate(otherEntity.getBrain(), pos));
    }

    private static boolean isMobComingThroughGate(Brain<?> brain, BlockPos pos) {
        if (!brain.hasMemoryValue(MemoryModuleType.PATH)) {
            return false;
        } else {
            Path path = brain.getMemory(MemoryModuleType.PATH).get();
            if (path.isDone()) {
                return false;
            } else {
                Node node = path.getPreviousNode();
                if (node == null) {
                    return false;
                } else {
                    Node node1 = path.getNextNode();
                    return pos.equals(node.asBlockPos()) || pos.equals(node1.asBlockPos());
                }
            }
        }
    }

    private static boolean isGateTooFarAway(ServerLevel level, LivingEntity entity, GlobalPos pos) {
        return pos.dimension() != level.dimension() || !pos.pos().closerToCenterThan(entity.position(), SKIP_CLOSING_GATE_IF_FURTHER_AWAY_THAN);
    }

    private static Optional<Set<GlobalPos>> rememberGateToClose(MemoryAccessor<OptionalBox.Mu, Set<GlobalPos>> gatesToClose, Optional<Set<GlobalPos>> gatePositions, ServerLevel level, BlockPos pos) {
        GlobalPos globalpos = GlobalPos.of(level.dimension(), pos);
        return Optional.of(gatePositions.map((positions) -> {
            positions.add(globalpos);
            return positions;
        }).orElseGet(() -> {
            Set<GlobalPos> set = Sets.newHashSet(globalpos);
            gatesToClose.set(set);
            return set;
        }));
    }
}