package org.sosly.villagetale.network.packets.clientbound;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.sosly.villagetale.VillageTale;
import org.sosly.villagetale.client.gui.TownHallScreen;
import org.sosly.villagetale.network.BasePacket;
import org.sosly.villagetale.network.ClientPacketHandler;

import java.util.UUID;
import java.util.function.Supplier;

public class OpenTownHallScreenPacket extends BasePacket {
    private final UUID villageId;
    private final String villageName;

    public OpenTownHallScreenPacket(UUID villageId, String villageName) {
        this.villageId = villageId;
        this.villageName = villageName;
    }

    public static void encode(OpenTownHallScreenPacket msg, FriendlyByteBuf buffer) {
        buffer.writeUUID(msg.villageId);
        buffer.writeUtf(msg.villageName);
    }

    public static OpenTownHallScreenPacket decode(FriendlyByteBuf buffer) {
        OpenTownHallScreenPacket msg;

        try {
            UUID villageId = buffer.readUUID();
            String villageName = buffer.readUtf();
            msg = new OpenTownHallScreenPacket(villageId, villageName);
        } catch (IndexOutOfBoundsException | IllegalArgumentException err) {
            VillageTale.LOGGER.error("Exception while reading OpenTownHallScreenPacket: {}", err.toString());
            return null;
        }

        msg.messageIsValid = true;
        return msg;
    }

    public static void handle(OpenTownHallScreenPacket msg, Supplier<NetworkEvent.Context> ctx) {
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
