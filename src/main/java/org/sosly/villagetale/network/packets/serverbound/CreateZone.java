package org.sosly.villagetale.network.packets.serverbound;

import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraftforge.network.NetworkEvent;
import org.sosly.villagetale.VillageTale;
import org.sosly.villagetale.api.IZoneShape;
import org.sosly.villagetale.api.IZoneType;
import org.sosly.villagetale.api.capability.IVillageCapability;
import org.sosly.villagetale.api.capability.IVillagesCapability;
import org.sosly.villagetale.capability.Capabilities;
import org.sosly.villagetale.data.VillageInfo;
import org.sosly.villagetale.network.BasePacket;
import org.sosly.villagetale.network.NetworkHandler;
import org.sosly.villagetale.network.ServerPacketHandler;
import org.sosly.villagetale.network.packets.clientbound.SyncVillageCapability;
import org.sosly.villagetale.network.packets.clientbound.ZoneBoundary;
import org.sosly.villagetale.zone.Zone;
import org.sosly.villagetale.zone.ZoneRegistry;

public class CreateZone extends BasePacket {
    private final UUID villageId;
    private final String zoneName;
    private final ResourceLocation zoneType;
    private final CompoundTag shapeData;

    private CreateZone(UUID villageId, String zoneName, ResourceLocation zoneType, CompoundTag shapeData) {
        this.villageId = villageId;
        this.zoneName = zoneName;
        this.zoneType = zoneType;
        this.shapeData = shapeData;
    }

    public static void send(UUID villageId, String zoneName, ResourceLocation zoneType, IZoneShape shape) {
        CompoundTag shapeData = shape.serializeNBT();
        shapeData.putString("shapeId", shape.getID().toString());
        CreateZone packet = new CreateZone(villageId, zoneName, zoneType, shapeData);
        NetworkHandler.CHANNEL.sendToServer(packet);
    }

    public static void encode(CreateZone msg, FriendlyByteBuf buffer) {
        buffer.writeUUID(msg.villageId);
        buffer.writeUtf(msg.zoneName);
        buffer.writeResourceLocation(msg.zoneType);
        buffer.writeNbt(msg.shapeData);
    }

    public static CreateZone decode(FriendlyByteBuf buffer) {
        CreateZone msg;

        try {
            UUID villageId = buffer.readUUID();
            String zoneName = buffer.readUtf();
            ResourceLocation zoneType = buffer.readResourceLocation();
            CompoundTag shapeData = buffer.readNbt();
            msg = new CreateZone(villageId, zoneName, zoneType, shapeData);
        } catch (IndexOutOfBoundsException | IllegalArgumentException err) {
            VillageTale.LOGGER.error("Exception while reading CreateZone: {}", err.toString());
            return null;
        }

        msg.messageIsValid = true;
        return msg;
    }

    public static void handle(CreateZone msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        if (!ServerPacketHandler.validateBasics(msg, context)) {
            return;
        }

        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }

            ServerLevel level = player.serverLevel();
            IVillagesCapability villagesCapability = level.getCapability(Capabilities.VILLAGES_CAPABILITY).orElse(null);
            if (villagesCapability == null) {
                player.sendSystemMessage(Component.literal("Failed to create zone: capability not found"));
                return;
            }

            VillageInfo village = villagesCapability.getVillageById(msg.villageId);
            if (village == null) {
                player.sendSystemMessage(Component.literal("Village not found"));
                return;
            }

            ChunkPos villageChunk = village.getVillageStartingChunk();
            IVillageCapability villageCapability = level.getChunk(villageChunk.x, villageChunk.z)
                .getCapability(Capabilities.VILLAGE_CAPABILITY).orElse(null);

            if (villageCapability == null) {
                player.sendSystemMessage(Component.literal("Failed to create zone: capability not found"));
                return;
            }

            if (!villageCapability.hasPermission(player.getUUID(), IVillageCapability.Permission.OWNER)) {
                player.sendSystemMessage(Component.literal("You do not have permission to create zones in this village"));
                return;
            }

            IZoneType type = ZoneRegistry.INSTANCE.type(msg.zoneType);
            if (type == null) {
                player.sendSystemMessage(Component.literal("Invalid zone type: " + msg.zoneType));
                return;
            }

            ResourceLocation shapeId = new ResourceLocation(msg.shapeData.getString("shapeId"));
            IZoneShape shape = ZoneRegistry.INSTANCE.shape(shapeId);
            if (shape == null) {
                player.sendSystemMessage(Component.literal("Invalid zone shape: " + shapeId));
                return;
            }
            shape.deserializeNBT(msg.shapeData);

            Zone zone = new Zone(level, UUID.randomUUID(), villageCapability, villageCapability.getZones().size(), shape, type);
            zone.setName(msg.zoneName);
            type.initialize(level, shape);

            villageCapability.addZone(zone);
            player.sendSystemMessage(Component.literal("Zone created: " + zone.getName()));

            ZoneBoundary.sendToPlayer(player, zone.getUUID(), msg.villageId, shape);
            SyncVillageCapability.send(player, villageCapability, player.server);
        });
    }

    public UUID getVillageId() {
        return villageId;
    }

    public String getZoneName() {
        return zoneName;
    }

    public ResourceLocation getZoneType() {
        return zoneType;
    }

    public CompoundTag getShapeData() {
        return shapeData;
    }
}
