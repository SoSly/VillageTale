package org.sosly.villagetale.entity.ai.navigation;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import net.minecraftforge.common.Tags;

public class VillagerNodeEvaluator extends WalkNodeEvaluator {
    
    @Override
    public BlockPathTypes getBlockPathType(BlockGetter level, int x, int y, int z) {
        BlockPathTypes type = super.getBlockPathType(level, x, y, z);
        
        if (type == BlockPathTypes.FENCE) {
            BlockPos pos = new BlockPos(x, y, z);
            BlockState blockstate = level.getBlockState(pos);
            Block block = blockstate.getBlock();
            
            if (block instanceof FenceGateBlock || blockstate.is(Tags.Blocks.FENCE_GATES)) {
                return BlockPathTypes.DOOR_WOOD_CLOSED;
            }
        }
        
        return type;
    }
    
    @Override
    protected BlockPathTypes evaluateBlockPathType(BlockGetter level, BlockPos pos, BlockPathTypes pathTypes) {
        BlockPathTypes result = super.evaluateBlockPathType(level, pos, pathTypes);
        
        if (result == BlockPathTypes.DOOR_WOOD_CLOSED && this.canOpenDoors() && this.canPassDoors()) {
            result = BlockPathTypes.WALKABLE_DOOR;
        }
        
        return result;
    }
}