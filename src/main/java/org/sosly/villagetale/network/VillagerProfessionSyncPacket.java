package org.sosly.villagetale.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public class VillagerProfessionSyncPacket {
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
        int entityId = buffer.readVarInt();
        ResourceLocation professionId = buffer.readResourceLocation();
        return new VillagerProfessionSyncPacket(entityId, professionId);
    }

    public static void handle(VillagerProfessionSyncPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> ClientPacketHandler.handleProfessionSync(msg));
        ctx.get().setPacketHandled(true);
    }

    public int getEntityId() {
        return entityId;
    }

    public ResourceLocation getProfessionId() {
        return professionId;
    }
}