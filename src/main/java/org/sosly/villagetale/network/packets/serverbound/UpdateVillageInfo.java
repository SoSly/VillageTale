package org.sosly.villagetale.network.packets.serverbound;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraftforge.network.NetworkEvent;
import org.sosly.villagetale.VillageTale;
import org.sosly.villagetale.api.capability.IVillageCapability;
import org.sosly.villagetale.api.capability.IVillagesCapability;
import org.sosly.villagetale.capability.Capabilities;
import org.sosly.villagetale.data.VillageInfo;
import org.sosly.villagetale.network.BasePacket;
import org.sosly.villagetale.network.NetworkHandler;
import org.sosly.villagetale.network.ServerPacketHandler;

import java.util.UUID;
import java.util.function.Supplier;

public class UpdateVillageInfo extends BasePacket {
    private final UUID villageId;
    private final String newName;

    private UpdateVillageInfo(UUID villageId, String newName) {
        this.villageId = villageId;
        this.newName = newName;
    }

    public static void send(UUID villageId, String newName) {
        UpdateVillageInfo packet = new UpdateVillageInfo(villageId, newName);
        NetworkHandler.CHANNEL.sendToServer(packet);
    }

    public static void encode(UpdateVillageInfo msg, FriendlyByteBuf buffer) {
        buffer.writeUUID(msg.villageId);
        buffer.writeUtf(msg.newName);
    }

    public static UpdateVillageInfo decode(FriendlyByteBuf buffer) {
        UpdateVillageInfo msg;

        try {
            UUID villageId = buffer.readUUID();
            String newName = buffer.readUtf();
            msg = new UpdateVillageInfo(villageId, newName);
        } catch (IndexOutOfBoundsException | IllegalArgumentException err) {
            VillageTale.LOGGER.error("Exception while reading UpdateVillageInfo: {}", err.toString());
            return null;
        }

        msg.messageIsValid = true;
        return msg;
    }

    public static void handle(UpdateVillageInfo msg, Supplier<NetworkEvent.Context> ctx) {
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
                player.sendSystemMessage(Component.literal("Failed to update village: capability not found"));
                return;
            }

            if (msg.newName == null || msg.newName.trim().isEmpty()) {
                player.sendSystemMessage(Component.literal("Village name cannot be empty"));
                return;
            }

            if (msg.newName.length() > 64) {
                player.sendSystemMessage(Component.literal("Village name is too long (max 64 characters)"));
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
                player.sendSystemMessage(Component.literal("Failed to update village: capability not found"));
                return;
            }

            if (!villageCapability.hasPermission(player.getUUID(), IVillageCapability.Permission.OWNER)) {
                player.sendSystemMessage(Component.literal("You do not have permission to rename this village"));
                return;
            }

            boolean success = villagesCapability.updateVillageName(msg.villageId, msg.newName);
            if (success) {
                player.sendSystemMessage(Component.literal("Village renamed to: " + msg.newName));
            } else {
                player.sendSystemMessage(Component.literal("Failed to rename village (name may already be in use)"));
            }
        });
    }

    public UUID getVillageId() {
        return villageId;
    }

    public String getNewName() {
        return newName;
    }
}
