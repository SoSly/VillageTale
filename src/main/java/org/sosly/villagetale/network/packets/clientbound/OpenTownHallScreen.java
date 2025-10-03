package org.sosly.villagetale.network.packets.clientbound;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import org.sosly.villagetale.VillageTale;
import org.sosly.villagetale.client.gui.TownHallScreen;
import org.sosly.villagetale.network.BasePacket;
import org.sosly.villagetale.network.ClientPacketHandler;
import org.sosly.villagetale.network.NetworkHandler;

import java.util.UUID;
import java.util.function.Supplier;

public class OpenTownHallScreen extends BasePacket {
    private final UUID villageId;
    private final String villageName;

    private OpenTownHallScreen(UUID villageId, String villageName) {
        this.villageId = villageId;
        this.villageName = villageName;
    }

    public static void send(ServerPlayer player, UUID villageId, String villageName) {
        OpenTownHallScreen packet = new OpenTownHallScreen(villageId, villageName);
        NetworkHandler.CHANNEL.sendTo(packet, player.connection.connection, NetworkDirection.PLAY_TO_CLIENT);
    }

    public static void encode(OpenTownHallScreen msg, FriendlyByteBuf buffer) {
        buffer.writeUUID(msg.villageId);
        buffer.writeUtf(msg.villageName);
    }

    public static OpenTownHallScreen decode(FriendlyByteBuf buffer) {
        OpenTownHallScreen msg;

        try {
            UUID villageId = buffer.readUUID();
            String villageName = buffer.readUtf();
            msg = new OpenTownHallScreen(villageId, villageName);
        } catch (IndexOutOfBoundsException | IllegalArgumentException err) {
            VillageTale.LOGGER.error("Exception while reading OpenTownHallScreen: {}", err.toString());
            return null;
        }

        msg.messageIsValid = true;
        return msg;
    }

    public static void handle(OpenTownHallScreen msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        if (!ClientPacketHandler.validateBasics(msg, context)) {
            return;
        }

        context.enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            mc.setScreen(new TownHallScreen(msg.villageId, msg.villageName));
        });
    }

    public UUID getVillageId() {
        return villageId;
    }

    public String getVillageName() {
        return villageName;
    }
}
