package org.sosly.villagetale.network.packets.clientbound;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import org.sosly.villagetale.VillageTale;
import org.sosly.villagetale.client.gui.VillageInfoScreen;
import org.sosly.villagetale.network.BasePacket;
import org.sosly.villagetale.network.ClientPacketHandler;
import org.sosly.villagetale.network.NetworkHandler;

import java.util.UUID;
import java.util.function.Supplier;

public class OpenVillageInfoScreen extends BasePacket {
    private final UUID villageId;

    private OpenVillageInfoScreen(UUID villageId) {
        this.villageId = villageId;
    }

    public static void send(ServerPlayer player, UUID villageId) {
        OpenVillageInfoScreen packet = new OpenVillageInfoScreen(villageId);
        NetworkHandler.CHANNEL.sendTo(packet, player.connection.connection, NetworkDirection.PLAY_TO_CLIENT);
    }

    public static void encode(OpenVillageInfoScreen msg, FriendlyByteBuf buffer) {
        buffer.writeUUID(msg.villageId);
    }

    public static OpenVillageInfoScreen decode(FriendlyByteBuf buffer) {
        OpenVillageInfoScreen msg;

        try {
            UUID villageId = buffer.readUUID();
            msg = new OpenVillageInfoScreen(villageId);
        } catch (IndexOutOfBoundsException | IllegalArgumentException err) {
            VillageTale.LOGGER.error("Exception while reading OpenVillageInfoScreen: {}", err.toString());
            return null;
        }

        msg.messageIsValid = true;
        return msg;
    }

    public static void handle(OpenVillageInfoScreen msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        if (!ClientPacketHandler.validateBasics(msg, context)) {
            return;
        }

        context.enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            mc.setScreen(new VillageInfoScreen(msg.villageId));
        });
    }

    public UUID getVillageId() {
        return villageId;
    }
}
