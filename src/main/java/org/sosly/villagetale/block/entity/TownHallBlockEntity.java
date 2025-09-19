package org.sosly.villagetale.block.entity;

import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.sosly.villagetale.VillageTale;
import org.sosly.villagetale.api.capability.IVillageCapability;
import org.sosly.villagetale.api.capability.IVillagesCapability;
import org.sosly.villagetale.api.IVillageZone;
import org.sosly.villagetale.capability.Capabilities;
import org.sosly.villagetale.data.VillageInfo;
import org.sosly.villagetale.zone.shape.Point;
import org.sosly.villagetale.zone.type.TownHall;

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

        IVillagesCapability villagesCapability = serverLevel.getCapability(Capabilities.VILLAGES_CAPABILITY).orElse(null);
        if (villagesCapability != null) {
            handleVillageSetup(serverLevel, chunkPos, player, villagesCapability);
        }
    }

    private void handleVillageSetup(ServerLevel level, ChunkPos chunkPos, Player player,
                                   IVillagesCapability villagesCapability) {
        VillageInfo existingVillage = villagesCapability.getVillageAt(chunkPos);

        if (existingVillage != null) {
            handleExistingVillage(level, existingVillage, player);
            return;
        }

        createNewVillage(level, chunkPos, player, villagesCapability);
    }

    private void handleExistingVillage(ServerLevel level, VillageInfo village, Player player) {
        BlockPos existingTownHall = village.getTownHallPos();

        if (existingTownHall != null) {
            VillageTale.LOGGER.error("Cannot place Town Hall at {} - village {} already has one at {}", worldPosition, village.getVillageName(), existingTownHall);
            if (player != null) {
                player.sendSystemMessage(Component.translatable("villagetale.townhall.already_exists", village.getVillageName(), existingTownHall.toShortString()));
            }
            level.destroyBlock(worldPosition, true);
            return;
        }

        IVillagesCapability villagesCapability = level.getCapability(Capabilities.VILLAGES_CAPABILITY).orElse(null);
        if (villagesCapability == null) {
            return;
        }

        villagesCapability.updateTownHallPos(village.getVillageId(), worldPosition);
        setVillageId(village.getVillageId());
        createTownHallZone(level, village);

        VillageTale.LOGGER.info("Added town hall for village {} at {}", village.getVillageName(), worldPosition);

        if (player != null) {
            player.sendSystemMessage(Component.literal("Added town hall for village " + village.getVillageName()));
        }
    }

    private void createNewVillage(ServerLevel level, ChunkPos chunkPos, Player player,
                                 IVillagesCapability villagesCapability) {
        String villageName = player != null
            ? Component.translatable("villagetale.village.default_name", player.getName().getString()).getString()
            : "Village_" + System.currentTimeMillis();

        UUID newVillageId = villagesCapability.createVillage(worldPosition, villageName, 3);

        if (newVillageId == null) {
            VillageTale.LOGGER.error("Failed to create village at {} - likely too close to another village", worldPosition);
            if (player != null) {
                player.sendSystemMessage(Component.translatable("villagetale.townhall.too_close"));
            }
            level.destroyBlock(worldPosition, true);
            return;
        }

        setVillageId(newVillageId);

        IVillageCapability cap = level.getChunk(chunkPos.x, chunkPos.z).getCapability(Capabilities.VILLAGE_CAPABILITY).orElse(null);
        if (cap == null) {
            return;
        }
        
        cap.setUUID(newVillageId);
        cap.setName(villageName);

        VillageInfo village = villagesCapability.getVillageById(newVillageId);
        if (village != null) {
            createTownHallZone(level, village);
        }

        VillageTale.LOGGER.info("Created village {} at {} with ID {} and town hall at {}",
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

        IVillageZone townHallZone = Point.builder(level, village, zones.size())
            .setPos(worldPosition)
            .setType(TownHall.ID)
            .build();
        townHallZone.setName("Town Hall");

        village.addZone(townHallZone);
        VillageTale.LOGGER.info("Created TownHall zone at {} for village {}", worldPosition, village.getName());
    }

}
