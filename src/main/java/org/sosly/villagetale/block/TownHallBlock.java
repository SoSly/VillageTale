package org.sosly.villagetale.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
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
import org.sosly.villagetale.VillageTale;
import org.sosly.villagetale.api.data.ZoneType;
import org.sosly.villagetale.block.entity.TownHallBlockEntity;
import org.sosly.villagetale.capability.Capabilities;
import org.sosly.villagetale.data.VillageInfo;

import java.util.UUID;

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
        if (state.is(newState.getBlock()) || level.isClientSide) {
            super.onRemove(state, level, pos, newState, isMoving);
            return;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof TownHallBlockEntity townHall) || !(level instanceof ServerLevel serverLevel)) {
            super.onRemove(state, level, pos, newState, isMoving);
            return;
        }

        UUID villageId = townHall.getVillageId();
        if (villageId == null) {
            super.onRemove(state, level, pos, newState, isMoving);
            return;
        }

        var villagesCapability = serverLevel.getCapability(Capabilities.VILLAGES_CAPABILITY).orElse(null);
        if (villagesCapability == null) {
            super.onRemove(state, level, pos, newState, isMoving);
            return;
        }

        VillageInfo village = villagesCapability.getVillageById(villageId);
        if (village == null) {
            super.onRemove(state, level, pos, newState, isMoving);
            return;
        }

        village.setTownHallPos(null);
        removeTownHallZone(serverLevel, village);
        VillageTale.LOGGER.info("Removed town hall at {} from village {}", pos, village.getVillageName());

        super.onRemove(state, level, pos, newState, isMoving);
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

    private void removeTownHallZone(ServerLevel level, VillageInfo village) {
        var chunk = level.getChunk(village.getVillageStartingChunk().x, village.getVillageStartingChunk().z);
        var villageCapability = chunk.getCapability(Capabilities.VILLAGE_CAPABILITY).orElse(null);
        if (villageCapability != null) {
            var zones = villageCapability.getZones();
            zones.removeIf(zone -> zone.getType() == ZoneType.TOWNHALL);
            VillageTale.LOGGER.info("Removed TOWNHALL zone from village {}", village.getVillageName());
        }
    }
}
