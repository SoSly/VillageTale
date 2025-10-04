package org.sosly.villagetale.block;

import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.network.NetworkDirection;
import org.jetbrains.annotations.Nullable;
import org.sosly.villagetale.VillageTale;
import org.sosly.villagetale.api.capability.IVillageCapability;
import org.sosly.villagetale.api.capability.IVillagesCapability;
import org.sosly.villagetale.api.IVillageZone;
import net.minecraft.world.level.ChunkPos;
import org.sosly.villagetale.capability.Capabilities;
import org.sosly.villagetale.data.VillageInfo;
import org.sosly.villagetale.item.LedgerItem;
import org.sosly.villagetale.network.NetworkHandler;
import org.sosly.villagetale.network.packets.clientbound.OpenTownHallScreen;
import org.sosly.villagetale.zone.type.TownHall;
import org.sosly.villagetale.entity.Villager;
import net.minecraft.world.phys.AABB;

import java.util.List;

public class TownHallBlock extends Block {
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

    private List<Villager> findFollowerVillagers(ServerLevel level, Player player) {
        AABB searchArea = player.getBoundingBox().inflate(32.0D);
        return level.getEntitiesOfClass(Villager.class, searchArea, villager ->
            villager.getFollowingPlayer().orElse(null) != null &&
            villager.getFollowingPlayer().get().equals(player.getUUID())
        );
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        ItemStack heldItem = player.getItemInHand(hand);
        if (!(heldItem.getItem() instanceof LedgerItem)) {
            return InteractionResult.PASS;
        }

        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.FAIL;
        }

        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.FAIL;
        }

        IVillagesCapability villagesCapability = serverLevel.getCapability(Capabilities.VILLAGES_CAPABILITY).orElse(null);
        if (villagesCapability == null) {
            return InteractionResult.FAIL;
        }

        VillageInfo village = villagesCapability.getVillageAt(new ChunkPos(pos));
        if (village == null || village.getTownHallPos() == null || !village.getTownHallPos().equals(pos)) {
            player.sendSystemMessage(Component.literal("This Town Hall is not connected to a village"));
            return InteractionResult.SUCCESS;
        }

        ChunkPos villageChunk = village.getVillageStartingChunk();
        IVillageCapability villageCapability = serverLevel.getChunk(villageChunk.x, villageChunk.z)
            .getCapability(Capabilities.VILLAGE_CAPABILITY).orElse(null);

        if (villageCapability == null) {
            return InteractionResult.FAIL;
        }

        if (!villageCapability.hasPermission(player.getUUID(), IVillageCapability.Permission.OWNER)) {
            player.sendSystemMessage(Component.literal("You do not have permission to manage this village"));
            return InteractionResult.SUCCESS;
        }

        List<Villager> followers = findFollowerVillagers(serverLevel, player);
        if (!followers.isEmpty()) {
            int assignedCount = 0;
            for (Villager follower : followers) {
                follower.setVillage(village.getVillageId());
                follower.setFollowingPlayer(null);
                assignedCount++;
            }
            player.sendSystemMessage(Component.literal("Assigned " + assignedCount + " villager(s) to " + village.getVillageName()));
            return InteractionResult.SUCCESS;
        }

        LedgerItem.setVillageUUID(heldItem, village.getVillageId());

        OpenTownHallScreen.send(serverPlayer, village.getVillageId(), village.getVillageName());

        return InteractionResult.SUCCESS;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (state.is(newState.getBlock()) || level.isClientSide) {
            super.onRemove(state, level, pos, newState, isMoving);
            return;
        }

        if (!(level instanceof ServerLevel serverLevel)) {
            super.onRemove(state, level, pos, newState, isMoving);
            return;
        }

        IVillagesCapability villagesCapability = serverLevel.getCapability(Capabilities.VILLAGES_CAPABILITY).orElse(null);
        if (villagesCapability == null) {
            super.onRemove(state, level, pos, newState, isMoving);
            return;
        }

        VillageInfo village = villagesCapability.getVillageAt(new ChunkPos(pos));
        if (village == null || village.getTownHallPos() == null || !village.getTownHallPos().equals(pos)) {
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

        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        onPlaced(serverLevel, pos, player);
    }

    private void onPlaced(ServerLevel level, BlockPos pos, Player player) {
        ChunkPos chunkPos = new ChunkPos(pos);

        IVillagesCapability villagesCapability = level.getCapability(Capabilities.VILLAGES_CAPABILITY)
                .orElseThrow(NullPointerException::new);
        handleVillageSetup(level, pos, chunkPos, player, villagesCapability);
    }

    private void handleVillageSetup(ServerLevel level, BlockPos pos, ChunkPos chunkPos, Player player,
                                   IVillagesCapability villagesCapability) {
        VillageInfo existingVillage = villagesCapability.getVillageAt(chunkPos);

        if (existingVillage != null) {
            handleExistingVillage(level, pos, existingVillage, player);
            return;
        }

        createNewVillage(level, pos, chunkPos, player, villagesCapability);
    }

    private void handleExistingVillage(ServerLevel level, BlockPos pos, VillageInfo village, Player player) {
        BlockPos existingTownHall = village.getTownHallPos();

        if (existingTownHall != null) {
            VillageTale.LOGGER.error("Cannot place Town Hall at {} - village {} already has one at {}", pos, village.getVillageName(), existingTownHall);
            if (player != null) {
                player.sendSystemMessage(Component.translatable("villagetale.townhall.already_exists", village.getVillageName(), existingTownHall.toShortString()));
            }
            level.destroyBlock(pos, true);
            return;
        }

        IVillagesCapability villagesCapability = level.getCapability(Capabilities.VILLAGES_CAPABILITY)
                .orElseThrow(NullPointerException::new);
        villagesCapability.updateTownHallPos(village.getVillageId(), pos);
        createTownHallZone(level, village);

        VillageTale.LOGGER.info("Added town hall for village {} at {}", village.getVillageName(), pos);

        if (player != null) {
            player.sendSystemMessage(Component.literal("Added town hall for village " + village.getVillageName()));
        }
    }

    private void createNewVillage(ServerLevel level, BlockPos pos, ChunkPos chunkPos, Player player,
                                 IVillagesCapability villagesCapability) {
        String villageName = player != null
            ? Component.translatable("villagetale.village.default_name", player.getName().getString()).getString()
            : "Village_" + System.currentTimeMillis();

        UUID newVillageId = villagesCapability.createVillage(pos, villageName, 3);

        if (newVillageId == null) {
            VillageTale.LOGGER.error("Failed to create village at {} - likely too close to another village", pos);
            if (player != null) {
                player.sendSystemMessage(Component.translatable("villagetale.townhall.too_close"));
            }
            level.destroyBlock(pos, true);
            return;
        }

        IVillageCapability cap = level.getChunk(chunkPos.x, chunkPos.z).getCapability(Capabilities.VILLAGE_CAPABILITY)
                .orElseThrow(NullPointerException::new);
        cap.setUUID(newVillageId);
        cap.setName(villageName);

        if (player != null) {
            cap.setPlayerPermission(player.getUUID(), IVillageCapability.Permission.OWNER);
        }

        VillageInfo village = villagesCapability.getVillageById(newVillageId);
        if (village != null) {
            createTownHallZone(level, village);
        }

        VillageTale.LOGGER.info("Created village {} at {} with ID {} and town hall at {}",
            villageName, chunkPos, newVillageId, pos);
    }

    private void createTownHallZone(ServerLevel level, VillageInfo info) {
        LevelChunk chunk = level.getChunk(info.getVillageStartingChunk().x, info.getVillageStartingChunk().z);
        IVillageCapability village = chunk.getCapability(Capabilities.VILLAGE_CAPABILITY).orElse(null);
        if (village == null) {
            return;
        }

        List<IVillageZone> zones = village.getZones();
        IVillageZone oldTownHall = zones.stream()
                .filter(zone -> zone.getType().getID() == TownHall.ID)
                .findAny()
                .orElse(null);
        if (oldTownHall != null) {
            village.removeZone(oldTownHall.getUUID());
        }

        BlockPos townHallPos = info.getTownHallPos();
        if (townHallPos == null) {
            return;
        }

        IVillageZone townHallZone = org.sosly.villagetale.zone.shape.Point.builder(level, village, zones.size())
            .setPos(townHallPos)
            .setType(TownHall.ID)
            .build();
        townHallZone.setName("Town Hall");

        village.addZone(townHallZone);
        VillageTale.LOGGER.info("Created TownHall zone at {} for village {}", townHallPos, village.getName());
    }

    private void removeTownHallZone(ServerLevel level, VillageInfo info) {
        LevelChunk chunk = level.getChunk(info.getVillageStartingChunk().x, info.getVillageStartingChunk().z);
        IVillageCapability village = chunk.getCapability(Capabilities.VILLAGE_CAPABILITY).orElse(null);
        if (village == null) {
            return;
        }

        List<IVillageZone> zones = village.getZones();
        IVillageZone oldTownHall = zones.stream()
                .filter(zone -> zone.getType().getID() == TownHall.ID)
                .findAny()
                .orElse(null);
        if (oldTownHall != null) {
            village.removeZone(oldTownHall.getUUID());
        }

        VillageTale.LOGGER.info("Removed TownHall zone from village {}", village.getName());
    }
}
