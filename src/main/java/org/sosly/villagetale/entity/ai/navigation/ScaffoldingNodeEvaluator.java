package org.sosly.villagetale.entity.ai.navigation;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;

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
        
        neighborCount = tryAddScaffoldingNeighbor(outputArray, neighborCount, nodePos.above(), Direction.UP);
        neighborCount = tryAddScaffoldingNeighbor(outputArray, neighborCount, nodePos.below(), Direction.DOWN);
        
        return neighborCount;
    }
    
    private int tryAddScaffoldingNeighbor(Node[] outputArray, int neighborCount, BlockPos targetPos, Direction direction) {
        BlockState targetState = this.level.getBlockState(targetPos);
        if (!targetState.is(Blocks.SCAFFOLDING)) {
            return neighborCount;
        }
        
        Node targetNode = this.getNode(targetPos);
        if (targetNode == null || targetNode.closed) {
            return neighborCount;
        }
        
        if (direction == Direction.DOWN || canMoveToScaffolding(targetPos)) {
            targetNode.type = BlockPathTypes.WALKABLE;
            targetNode.costMalus = Math.max(targetNode.costMalus, 0.0F);
            outputArray[neighborCount++] = targetNode;
        }
        
        return neighborCount;
    }
    
    private boolean canMoveToScaffolding(BlockPos pos) {
        BlockState state = this.level.getBlockState(pos);
        return state.is(Blocks.SCAFFOLDING) || state.isAir();
    }
}