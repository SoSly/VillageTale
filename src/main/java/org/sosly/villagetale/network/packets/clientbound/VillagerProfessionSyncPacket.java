package org.sosly.villagetale.network.packets.clientbound;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;
import org.sosly.villagetale.VillageTale;
import org.sosly.villagetale.client.ClientDataManager;
import org.sosly.villagetale.network.BasePacket;
import org.sosly.villagetale.network.ClientPacketHandler;

import java.util.function.Supplier;

public class VillagerProfessionSyncPacket extends BasePacket {
    private final int entityId;
    private final ResourceLocation professionId;

    public VillagerProfessionSyncPacket(int entityId, ResourceLocation professionId) {
        this.entityId = entityId;
        this.professionId = professionId;
    }

    public static void encode(VillagerProfessionSyncPacket msg, FriendlyByteBuf buffer) {
        buffer.writeVarInt(msg.entityId);
        buffer.writeResourceLocation(msg.professionId);
    }

    public static VillagerProfessionSyncPacket decode(FriendlyByteBuf buffer) {
        VillagerProfessionSyncPacket msg;

        try {
            int entityId = buffer.readVarInt();
            ResourceLocation professionId = buffer.readResourceLocation();
            msg = new VillagerProfessionSyncPacket(entityId, professionId);
        } catch (IndexOutOfBoundsException | IllegalArgumentException err) {
            VillageTale.LOGGER.error("Exception while reading VillagerProfessionSyncPacket: {}", err.toString());
            return null;
        }

        msg.messageIsValid = true;
        return msg;
    }

    public static void handle(VillagerProfessionSyncPacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        if (ClientPacketHandler.validateBasics(msg, context)) {
            context.enqueueWork(() -> {
                Minecraft mc = Minecraft.getInstance();
                if (mc.level == null) {
                    return;
                }

                Entity entity = mc.level.getEntity(msg.entityId);
                if (entity != null) {
                    ClientDataManager.cacheProfession(msg.entityId, msg.professionId);
                }
            });
        }
    }

    public int getEntityId() {
        return entityId;
    }

    public ResourceLocation getProfessionId() {
        return professionId;
    }
}
