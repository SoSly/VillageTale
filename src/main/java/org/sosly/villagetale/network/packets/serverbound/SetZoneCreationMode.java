package org.sosly.villagetale.network.packets.serverbound;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import org.sosly.villagetale.VillageTale;
import org.sosly.villagetale.network.BasePacket;
import org.sosly.villagetale.network.NetworkHandler;
import org.sosly.villagetale.network.ServerPacketHandler;
import org.sosly.villagetale.helper.ZoneCreationHelper;

import java.util.function.Supplier;

public class SetZoneCreationMode extends BasePacket {
    private final boolean active;

    private SetZoneCreationMode(boolean active) {
        this.active = active;
    }

    public static void send(boolean active) {
        SetZoneCreationMode packet = new SetZoneCreationMode(active);
        NetworkHandler.CHANNEL.sendToServer(packet);
    }

    public static void encode(SetZoneCreationMode msg, FriendlyByteBuf buffer) {
        buffer.writeBoolean(msg.active);
    }

    public static SetZoneCreationMode decode(FriendlyByteBuf buffer) {
        SetZoneCreationMode msg;

        try {
            boolean active = buffer.readBoolean();
            msg = new SetZoneCreationMode(active);
        } catch (IndexOutOfBoundsException | IllegalArgumentException err) {
            VillageTale.LOGGER.error("Exception while reading SetZoneCreationMode: {}", err.toString());
            return null;
        }

        msg.messageIsValid = true;
        return msg;
    }

    public static void handle(SetZoneCreationMode msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        if (!ServerPacketHandler.validateBasics(msg, context)) {
            return;
        }

        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }

            ZoneCreationHelper.getInstance().setCreationMode(player.getUUID(), msg.active);
            System.out.println("[SetZoneCreationMode] Server set zone creation mode to: " + msg.active + " for player " + player.getName().getString());
        });
    }

    public boolean isActive() {
        return active;
    }
}
