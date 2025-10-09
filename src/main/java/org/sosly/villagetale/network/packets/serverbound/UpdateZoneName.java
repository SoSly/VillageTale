package org.sosly.villagetale.network.packets.serverbound;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraftforge.network.NetworkEvent;
import org.sosly.villagetale.VillageTale;
import org.sosly.villagetale.api.IVillageZone;
import org.sosly.villagetale.api.capability.IVillageCapability;
import org.sosly.villagetale.api.capability.IVillagesCapability;
import org.sosly.villagetale.capability.Capabilities;
import org.sosly.villagetale.data.VillageInfo;
import org.sosly.villagetale.network.BasePacket;
import org.sosly.villagetale.network.NetworkHandler;
import org.sosly.villagetale.network.ServerPacketHandler;
import org.sosly.villagetale.network.packets.clientbound.SyncVillageCapability;

import java.util.UUID;
import java.util.function.Supplier;

public class UpdateZoneName extends BasePacket {
    private final UUID villageId;
    private final UUID zoneId;
    private final String newName;

    private UpdateZoneName(UUID villageId, UUID zoneId, String newName) {
        this.villageId = villageId;
        this.zoneId = zoneId;
        this.newName = newName;
    }

    public static void send(UUID villageId, UUID zoneId, String newName) {
        UpdateZoneName packet = new UpdateZoneName(villageId, zoneId, newName);
        NetworkHandler.CHANNEL.sendToServer(packet);
    }

    public static void encode(UpdateZoneName msg, FriendlyByteBuf buffer) {
        buffer.writeUUID(msg.villageId);
        buffer.writeUUID(msg.zoneId);
        buffer.writeUtf(msg.newName);
    }

    public static UpdateZoneName decode(FriendlyByteBuf buffer) {
        UpdateZoneName msg;

        try {
            UUID villageId = buffer.readUUID();
            UUID zoneId = buffer.readUUID();
            String newName = buffer.readUtf();
            msg = new UpdateZoneName(villageId, zoneId, newName);
        } catch (IndexOutOfBoundsException | IllegalArgumentException err) {
            VillageTale.LOGGER.error("Exception while reading UpdateZoneName: {}", err.toString());
            return null;
        }

        msg.messageIsValid = true;
        return msg;
    }

    public static void handle(UpdateZoneName msg, Supplier<NetworkEvent.Context> ctx) {
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
                player.sendSystemMessage(Component.literal("Failed to update zone: capability not found"));
                return;
            }

            if (msg.newName == null || msg.newName.trim().isEmpty()) {
                player.sendSystemMessage(Component.literal("Zone name cannot be empty"));
                return;
            }

            if (msg.newName.length() > 64) {
                player.sendSystemMessage(Component.literal("Zone name is too long (max 64 characters)"));
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
                player.sendSystemMessage(Component.literal("Failed to update zone: capability not found"));
                return;
            }

            if (!villageCapability.hasPermission(player.getUUID(), IVillageCapability.Permission.OWNER)) {
                player.sendSystemMessage(Component.literal("You do not have permission to rename zones in this village"));
                return;
            }

            IVillageZone zone = villageCapability.getZones().stream()
                .filter(z -> z.getUUID().equals(msg.zoneId))
                .findFirst()
                .orElse(null);

            if (zone == null) {
                player.sendSystemMessage(Component.literal("Zone not found"));
                return;
            }

            zone.setName(msg.newName);
            player.sendSystemMessage(Component.literal("Zone renamed to: " + msg.newName));

            SyncVillageCapability.send(player, villageCapability, player.server);
        });
    }

    public UUID getVillageId() {
        return villageId;
    }

    public UUID getZoneId() {
        return zoneId;
    }

    public String getNewName() {
        return newName;
    }
}
