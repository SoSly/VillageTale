package org.sosly.villagetale.entity.ai.navigation;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathFinder;
import net.minecraft.world.phys.Vec3;
import org.sosly.villagetale.entity.Villager;

public class VillagerNavigation extends GroundPathNavigation {
    public VillagerNavigation(Villager villager, Level level) {
        super(villager, level);
    }

    @Override
    protected PathFinder createPathFinder(int maxVisitedNodes) {
        this.nodeEvaluator = new ScaffoldingNodeEvaluator();
        this.nodeEvaluator.setCanPassDoors(true);
        return new PathFinder(this.nodeEvaluator, maxVisitedNodes);
    }

    @Override
    protected boolean canMoveDirectly(Vec3 from, Vec3 to) {
        BlockPos toPos = new BlockPos((int) to.x, (int) to.y, (int) to.z);
        BlockPos belowPos = toPos.below();
        BlockState belowState = this.level.getBlockState(belowPos);
        
        if (belowState.is(Blocks.SCAFFOLDING)) {
            return true;
        }
        
        return super.canMoveDirectly(from, to);
    }
    
    @Override
    protected boolean canUpdatePath() {
        return super.canUpdatePath();
    }
    
    @Override
    public boolean isStableDestination(BlockPos pos) {
        BlockState state = this.level.getBlockState(pos.below());
        if (state.is(Blocks.SCAFFOLDING)) {
            return true;
        }
        return super.isStableDestination(pos);
    }
}