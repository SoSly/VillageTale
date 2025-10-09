package org.sosly.villagetale.network.packets.clientbound;

import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;
import org.sosly.villagetale.VillageTale;
import org.sosly.villagetale.client.BoundaryDataStorage;
import org.sosly.villagetale.network.BasePacket;
import org.sosly.villagetale.network.ClientPacketHandler;
import org.sosly.villagetale.network.NetworkHandler;

public class RemoveZoneBoundary extends BasePacket {
    private final UUID zoneId;

    private RemoveZoneBoundary(UUID zoneId) {
        this.zoneId = zoneId;
    }

    public static void sendToPlayer(ServerPlayer player, UUID zoneId) {
        RemoveZoneBoundary packet = new RemoveZoneBoundary(zoneId);
        NetworkHandler.CHANNEL.send(
            PacketDistributor.PLAYER.with(() -> player),
            packet
        );
    }

    public static void encode(RemoveZoneBoundary msg, FriendlyByteBuf buffer) {
        buffer.writeUUID(msg.zoneId);
    }

    public static RemoveZoneBoundary decode(FriendlyByteBuf buffer) {
        RemoveZoneBoundary msg;

        try {
            UUID zoneId = buffer.readUUID();
            msg = new RemoveZoneBoundary(zoneId);
        } catch (IndexOutOfBoundsException | IllegalArgumentException err) {
            VillageTale.LOGGER.error("Exception while reading RemoveZoneBoundary: {}", err.toString());
            return null;
        }

        msg.messageIsValid = true;
        return msg;
    }

    public static void handle(RemoveZoneBoundary msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        if (ClientPacketHandler.validateBasics(msg, context)) {
            context.enqueueWork(() -> {
                Minecraft mc = Minecraft.getInstance();
                if (mc.level == null) {
                    return;
                }

                BoundaryDataStorage.getInstance().removeZone(mc.level.dimension(), msg.zoneId);
            });
        }
    }

    public UUID getZoneId() {
        return zoneId;
    }
}
