package org.sosly.villageworks.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.sosly.villageworks.VillageWorks;
import org.sosly.villageworks.api.capability.IVillagesCapability;
import org.sosly.villageworks.capability.Capabilities;
import org.sosly.villageworks.data.VillageData;

import java.util.UUID;

public class TownHallBlockEntity extends BlockEntity {
    private static final String NBT_VILLAGE_ID = "VillageId";

    private UUID villageId;

    public TownHallBlockEntity(BlockPos pos, BlockState blockState) {
        super(BlockEntityTypes.TOWNHALL.get(), pos, blockState);
    }

    public UUID getVillageId() {
        return villageId;
    }

    public void setVillageId(UUID villageId) {
        this.villageId = villageId;
        setChanged();
    }

    public void onPlaced(Player player) {
        if (level == null || level.isClientSide || !(level instanceof ServerLevel serverLevel)) {
            return;
        }

        ChunkPos chunkPos = new ChunkPos(worldPosition);

        serverLevel.getCapability(Capabilities.VILLAGES_CAPABILITY).ifPresent(villagesCapability ->
            handleVillageSetup(serverLevel, chunkPos, player, villagesCapability)
        );
    }

    private void handleVillageSetup(ServerLevel level, ChunkPos chunkPos, Player player,
                                   IVillagesCapability villagesCapability) {
        VillageData existingVillage = villagesCapability.getVillageAt(chunkPos);

        if (existingVillage != null) {
            handleExistingVillage(level, existingVillage, player);
            return;
        }

        createNewVillage(level, chunkPos, player, villagesCapability);
    }

    private void handleExistingVillage(ServerLevel level, VillageData village, Player player) {
        if (village.getTownHallBlockPos() != null) {
            VillageWorks.LOGGER.error("Cannot place Town Hall at {} - village {} already has one at {}",
                worldPosition, village.getVillageName(), village.getTownHallBlockPos());
            if (player != null) {
                player.sendSystemMessage(Component.translatable("villageworks.townhall.already_exists",
                    village.getVillageName(), village.getTownHallBlockPos().toShortString()));
            }
            level.destroyBlock(worldPosition, true);
            return;
        }

        VillageWorks.LOGGER.info("Town Hall placed at {} for existing village {}",
            worldPosition, village.getVillageName());
        village.setTownHallBlockPos(worldPosition);
        setVillageId(village.getVillageId());
    }

    private void createNewVillage(ServerLevel level, ChunkPos chunkPos, Player player,
                                 IVillagesCapability villagesCapability) {
        String villageName = player != null
            ? Component.translatable("villageworks.village.default_name", player.getName().getString()).getString()
            : "Village_" + System.currentTimeMillis();

        UUID newVillageId = villagesCapability.createVillage(chunkPos, villageName, 3);

        if (newVillageId == null) {
            VillageWorks.LOGGER.error("Failed to create village at {} - likely too close to another village", worldPosition);
            if (player != null) {
                player.sendSystemMessage(Component.translatable("villageworks.townhall.too_close"));
            }
            level.destroyBlock(worldPosition, true);
            return;
        }

        setVillageId(newVillageId);
        VillageData newVillage = villagesCapability.getVillageById(newVillageId);
        if (newVillage != null) {
            newVillage.setTownHallBlockPos(worldPosition);
        }
        VillageWorks.LOGGER.info("Created village {} at {} with ID {} and town hall at {}",
            villageName, chunkPos, newVillageId, worldPosition);
    }


    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (villageId != null) {
            tag.putUUID(NBT_VILLAGE_ID, villageId);
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.hasUUID(NBT_VILLAGE_ID)) {
            villageId = tag.getUUID(NBT_VILLAGE_ID);
        }
    }
}
