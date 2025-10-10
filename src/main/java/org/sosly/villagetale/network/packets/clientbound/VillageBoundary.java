package org.sosly.villagetale.network.packets.clientbound;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;
import org.sosly.villagetale.VillageTale;
import org.sosly.villagetale.client.BoundaryDataStorage;
import org.sosly.villagetale.data.VillageBoundaryData;
import org.sosly.villagetale.network.BasePacket;
import org.sosly.villagetale.network.ClientPacketHandler;

import java.util.UUID;
import java.util.function.Supplier;
import org.sosly.villagetale.network.NetworkHandler;

public class VillageBoundary extends BasePacket {
    private final UUID villageId;
    private final ChunkPos centerChunk;
    private final int squadius;

    public VillageBoundary(UUID villageId, ChunkPos centerChunk, int squadius) {
        this.villageId = villageId;
        this.centerChunk = centerChunk;
        this.squadius = squadius;
    }

    public static void encode(VillageBoundary msg, FriendlyByteBuf buffer) {
        buffer.writeUUID(msg.villageId);
        buffer.writeVarInt(msg.centerChunk.x);
        buffer.writeVarInt(msg.centerChunk.z);
        buffer.writeVarInt(msg.squadius);
    }

    public static VillageBoundary decode(FriendlyByteBuf buffer) {
        VillageBoundary msg;

        try {
            UUID villageId = buffer.readUUID();
            int chunkX = buffer.readVarInt();
            int chunkZ = buffer.readVarInt();
            int squadius = buffer.readVarInt();
            msg = new VillageBoundary(villageId, new ChunkPos(chunkX, chunkZ), squadius);
        } catch (IndexOutOfBoundsException | IllegalArgumentException err) {
            VillageTale.LOGGER.error("Exception while reading VillageBoundary: {}", err.toString());
            return null;
        }

        msg.messageIsValid = true;
        return msg;
    }

    public static void send(ServerLevel level, UUID villageId, ChunkPos centerChunk, int squadius) {
        VillageBoundary packet = new VillageBoundary(villageId, centerChunk, squadius);
        NetworkHandler.CHANNEL.send(PacketDistributor.DIMENSION.with(level::dimension), packet);
    }

    public static void handle(VillageBoundary msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        if (ClientPacketHandler.validateBasics(msg, context)) {
            context.enqueueWork(() -> {
                Minecraft mc = Minecraft.getInstance();
                if (mc.level == null) {
                    return;
                }

                VillageBoundaryData data = new VillageBoundaryData(
                    mc.level,
                    msg.villageId,
                    msg.centerChunk,
                    msg.squadius
                );
                BoundaryDataStorage.getInstance().addVillage(mc.level.dimension(), data);
            }).whenComplete((r, e) -> {
                if (e != null) {
                    throw new RuntimeException("Failed to handle VillageBoundary", e);
                }
            });
        }
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
