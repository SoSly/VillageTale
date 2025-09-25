package org.sosly.villagetale.entity.ai.navigation;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.entity.Mob;

public class ScaffoldingNodeEvaluator extends WalkNodeEvaluator {
    @Override
    protected BlockPathTypes evaluateBlockPathType(BlockGetter level, BlockPos pos, BlockPathTypes pathType) {
        if (pathType == BlockPathTypes.OPEN && level.getBlockState(pos).is(Blocks.SCAFFOLDING)) {
            return BlockPathTypes.WALKABLE;
        }
        return super.evaluateBlockPathType(level, pos, pathType);
    }
    
    @Override
    public BlockPathTypes getBlockPathType(BlockGetter level, int x, int y, int z) {
        BlockPos pos = new BlockPos(x, y, z);
        BlockState state = level.getBlockState(pos);
        
        if (state.is(Blocks.SCAFFOLDING)) {
            return BlockPathTypes.WALKABLE;
        }
        
        return super.getBlockPathType(level, x, y, z);
    }
    
    @Override
    public int getNeighbors(Node[] outputArray, Node node) {
        int neighborCount = super.getNeighbors(outputArray, node);
        BlockPos nodePos = node.asBlockPos();
        BlockState currentState = this.level.getBlockState(nodePos);
        BlockState belowState = this.level.getBlockState(nodePos.below());
        
        if (!currentState.is(Blocks.SCAFFOLDING) && belowState.is(Blocks.SCAFFOLDING)) {
            neighborCount = addNodeIfValid(outputArray, neighborCount, nodePos.below(), 0.5F);
        }
        
        if (!currentState.is(Blocks.SCAFFOLDING)) {
            return neighborCount;
        }
        
        BlockState aboveState = this.level.getBlockState(nodePos.above());
        if (aboveState.is(Blocks.SCAFFOLDING) || aboveState.isAir()) {
            neighborCount = addNodeIfValid(outputArray, neighborCount, nodePos.above(), 1.5F);
        }
        
        if (belowState.is(Blocks.SCAFFOLDING)) {
            neighborCount = addNodeIfValid(outputArray, neighborCount, nodePos.below(), 0.5F);
        }
        
        return neighborCount;
    }
    
    private int addNodeIfValid(Node[] outputArray, int neighborCount, BlockPos pos, float cost) {
        Node node = this.getNode(pos);
        if (node == null || node.closed) {
            return neighborCount;
        }
        
        node.type = BlockPathTypes.WALKABLE;
        node.costMalus = cost;
        outputArray[neighborCount++] = node;
        return neighborCount;
    }
}