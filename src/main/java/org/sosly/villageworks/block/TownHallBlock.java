package org.sosly.villageworks.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;
import org.sosly.villageworks.VillageWorks;
import org.sosly.villageworks.block.entity.TownHallBlockEntity;
import org.sosly.villageworks.capability.Capabilities;
import org.sosly.villageworks.data.VillageData;

public class TownHallBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    
    private static final VoxelShape SHAPE = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 16.0D, 16.0D);

    public TownHallBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof TownHallBlockEntity townHall)) {
            return InteractionResult.FAIL;
        }

        if (townHall.getVillageId() == null) {
            player.sendSystemMessage(Component.literal("This Town Hall is not connected to a village"));
            return InteractionResult.SUCCESS;
        }

        player.sendSystemMessage(Component.literal("Town Hall GUI not yet implemented. Village ID: " + townHall.getVillageId()));
        return InteractionResult.SUCCESS;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        super.onRemove(state, level, pos, newState, isMoving);
        
        if (state.is(newState.getBlock()) || level.isClientSide) {
            return;
        }
        
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof TownHallBlockEntity townHall)) {
            return;
        }
        
        if (townHall.getVillageId() == null || !(level instanceof ServerLevel serverLevel)) {
            return;
        }
        
        var villagesCapability = serverLevel.getCapability(Capabilities.VILLAGES_CAPABILITY).orElse(null);
        if (villagesCapability == null) {
            return;
        }
        
        VillageData village = villagesCapability.getVillageById(townHall.getVillageId());
        if (village != null && pos.equals(village.getTownHallPos())) {
            // Remove the entire village when its town hall is destroyed
            villagesCapability.removeVillage(townHall.getVillageId());
            VillageWorks.LOGGER.info("Removed village {} after town hall destruction at {}", 
                village.getVillageName(), pos);
        }
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable net.minecraft.world.entity.LivingEntity placer, net.minecraft.world.item.ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        
        if (level.isClientSide || !(placer instanceof Player player)) {
            return;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof TownHallBlockEntity townHall) {
            townHall.onPlaced(player);
        }
    }


    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TownHallBlockEntity(pos, state);
    }
}