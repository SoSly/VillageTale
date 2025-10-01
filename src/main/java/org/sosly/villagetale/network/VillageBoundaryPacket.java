package org.sosly.villagetale.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.ChunkPos;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public class VillageBoundaryPacket {
    private final UUID villageId;
    private final ChunkPos centerChunk;
    private final int squadius;

    public VillageBoundaryPacket(UUID villageId, ChunkPos centerChunk, int squadius) {
        this.villageId = villageId;
        this.centerChunk = centerChunk;
        this.squadius = squadius;
    }

    public static void encode(VillageBoundaryPacket msg, FriendlyByteBuf buffer) {
        buffer.writeUUID(msg.villageId);
        buffer.writeVarInt(msg.centerChunk.x);
        buffer.writeVarInt(msg.centerChunk.z);
        buffer.writeVarInt(msg.squadius);
    }

    public static VillageBoundaryPacket decode(FriendlyByteBuf buffer) {
        UUID villageId = buffer.readUUID();
        int chunkX = buffer.readVarInt();
        int chunkZ = buffer.readVarInt();
        int squadius = buffer.readVarInt();
        return new VillageBoundaryPacket(villageId, new ChunkPos(chunkX, chunkZ), squadius);
    }

    public static void handle(VillageBoundaryPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> ClientPacketHandler.handleVillageBoundary(msg));
        ctx.get().setPacketHandled(true);
    }

    public UUID getVillageId() {
        return villageId;
    }

    public ChunkPos getCenterChunk() {
        return centerChunk;
    }

    public int getSquadius() {
        return squadius;
    }
}
