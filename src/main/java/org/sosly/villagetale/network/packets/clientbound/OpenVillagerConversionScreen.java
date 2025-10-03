package org.sosly.villagetale.network.packets.clientbound;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import org.sosly.villagetale.VillageTale;
import org.sosly.villagetale.client.gui.VillagerConversionScreen;
import org.sosly.villagetale.network.BasePacket;
import org.sosly.villagetale.network.ClientPacketHandler;
import org.sosly.villagetale.network.NetworkHandler;

import java.util.function.Supplier;

public class OpenVillagerConversionScreen extends BasePacket {
    private final int villagerEntityId;

    private OpenVillagerConversionScreen(int villagerEntityId) {
        this.villagerEntityId = villagerEntityId;
    }

    public static void send(ServerPlayer player, int villagerEntityId) {
        OpenVillagerConversionScreen packet = new OpenVillagerConversionScreen(villagerEntityId);
        NetworkHandler.CHANNEL.sendTo(packet, player.connection.connection, NetworkDirection.PLAY_TO_CLIENT);
    }

    public static void encode(OpenVillagerConversionScreen msg, FriendlyByteBuf buffer) {
        buffer.writeInt(msg.villagerEntityId);
    }

    public static OpenVillagerConversionScreen decode(FriendlyByteBuf buffer) {
        OpenVillagerConversionScreen msg;

        try {
            int villagerEntityId = buffer.readInt();
            msg = new OpenVillagerConversionScreen(villagerEntityId);
        } catch (IndexOutOfBoundsException | IllegalArgumentException err) {
            VillageTale.LOGGER.error("Exception while reading OpenVillagerConversionScreen: {}", err.toString());
            return null;
        }

        msg.messageIsValid = true;
        return msg;
    }

    public static void handle(OpenVillagerConversionScreen msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        if (!ClientPacketHandler.validateBasics(msg, context)) {
            return;
        }

        context.enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            mc.setScreen(new VillagerConversionScreen(msg.villagerEntityId));
        });
    }

    public int getVillagerEntityId() {
        return villagerEntityId;
    }
}
