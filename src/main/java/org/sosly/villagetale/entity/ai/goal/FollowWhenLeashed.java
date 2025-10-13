package org.sosly.villagetale.entity.ai.goal;

import net.minecraft.world.entity.Entity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import org.sosly.villagetale.entity.Villager;

import java.util.EnumSet;

public class FollowWhenLeashed extends Goal {
    private static final double FOLLOW_DISTANCE_MIN = 2.0;
    private static final double FOLLOW_DISTANCE_MAX = 4.0;
    private static final double STOP_DISTANCE = 0.5;
    
    private final PathfinderMob animal;
    private Entity leashHolder;
    private final LevelReader level;
    private final double speedModifier;
    private final PathNavigation navigation;
    private int timeToRecalcPath;
    private final float maxDistance;
    private final float minDistance;
    private float oldWaterCost;
    
    public FollowWhenLeashed(PathfinderMob animal, double speedModifier) {
        this.animal = animal;
        this.level = animal.level();
        this.speedModifier = speedModifier;
        this.navigation = animal.getNavigation();
        this.minDistance = (float) FOLLOW_DISTANCE_MIN;
        this.maxDistance = (float) FOLLOW_DISTANCE_MAX;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }
    
    @Override
    public boolean canUse() {
        Entity holder = animal.getLeashHolder();
        if (holder == null) {
            return false;
        }
        
        if (!(holder instanceof Villager)) {
            return false;
        }
        
        if (animal.distanceToSqr(holder) < (double) (minDistance * minDistance)) {
            return false;
        }
        
        this.leashHolder = holder;
        return true;
    }
    
    @Override
    public boolean canContinueToUse() {
        if (!animal.isLeashed()) {
            return false;
        }
        
        if (animal.getLeashHolder() != leashHolder) {
            return false;
        }
        
        return animal.distanceToSqr(leashHolder) > (double) (STOP_DISTANCE * STOP_DISTANCE);
    }
    
    @Override
    public void start() {
        this.timeToRecalcPath = 0;
        this.oldWaterCost = animal.getPathfindingMalus(BlockPathTypes.WATER);
        animal.setPathfindingMalus(BlockPathTypes.WATER, 0.0F);
    }
    
    @Override
    public void stop() {
        this.leashHolder = null;
        this.navigation.stop();
        animal.setPathfindingMalus(BlockPathTypes.WATER, oldWaterCost);
    }
    
    @Override
    public void tick() {
        animal.getLookControl().setLookAt(leashHolder, 10.0F, (float) animal.getMaxHeadXRot());

        if (--timeToRecalcPath <= 0) {
            timeToRecalcPath = adjustedTickDelay(5);
            
            if (animal.distanceToSqr(leashHolder) >= 144.0D) {
                tryToTeleportNearLeashHolder();
            } else {
                navigation.moveTo(leashHolder, speedModifier);
            }
        }
    }
    
    private void tryToTeleportNearLeashHolder() {
        var blockPos = leashHolder.blockPosition();

        for (int i = 0; i < 10; ++i) {
            int xOffset = randomIntInclusive(-3, 3);
            int yOffset = randomIntInclusive(-1, 1);
            int zOffset = randomIntInclusive(-3, 3);
            boolean teleported = tryToTeleportToLocation(
                blockPos.getX() + xOffset, 
                blockPos.getY() + yOffset, 
                blockPos.getZ() + zOffset
            );
            if (teleported) {
                return;
            }
        }
    }
    
    private boolean tryToTeleportToLocation(int x, int y, int z) {
        if (Math.abs(x - leashHolder.getX()) < 2.0D && Math.abs(z - leashHolder.getZ()) < 2.0D) {
            return false;
        }
        
        if (!canTeleportTo(x, y, z)) {
            return false;
        }
        
        animal.moveTo(x + 0.5D, y, z + 0.5D, animal.getYRot(), animal.getXRot());
        navigation.stop();
        return true;
    }
    
    private boolean canTeleportTo(int x, int y, int z) {
        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos(x, y, z);
        var pathTypes = WalkNodeEvaluator.getBlockPathTypeStatic(level, mutablePos);
        if (pathTypes != BlockPathTypes.WALKABLE) {
            return false;
        }
        
        BlockState blockState = level.getBlockState(new BlockPos(x, y - 1, z));
        if (blockState.getBlock() instanceof LeavesBlock) {
            return false;
        }
        
        var blockPos = new BlockPos(x, y, z);
        if (!level.noCollision(animal, animal.getBoundingBox().move(blockPos))) {
            return false;
        }
        
        return true;
    }
    
    private int randomIntInclusive(int min, int max) {
        return animal.getRandom().nextInt(max - min + 1) + min;
    }
}
