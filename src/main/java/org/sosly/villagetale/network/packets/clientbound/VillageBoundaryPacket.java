package org.sosly.villagetale.network.packets.clientbound;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.ChunkPos;
import net.minecraftforge.network.NetworkEvent;
import org.sosly.villagetale.VillageTale;
import org.sosly.villagetale.client.BoundaryDataStorage;
import org.sosly.villagetale.data.VillageBoundaryData;
import org.sosly.villagetale.network.BasePacket;
import org.sosly.villagetale.network.ClientPacketHandler;

import java.util.UUID;
import java.util.function.Supplier;

public class VillageBoundaryPacket extends BasePacket {
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
        VillageBoundaryPacket msg;

        try {
            UUID villageId = buffer.readUUID();
            int chunkX = buffer.readVarInt();
            int chunkZ = buffer.readVarInt();
            int squadius = buffer.readVarInt();
            msg = new VillageBoundaryPacket(villageId, new ChunkPos(chunkX, chunkZ), squadius);
        } catch (IndexOutOfBoundsException | IllegalArgumentException err) {
            VillageTale.LOGGER.error("Exception while reading VillageBoundaryPacket: {}", err.toString());
            return null;
        }

        msg.messageIsValid = true;
        return msg;
    }

    public static void handle(VillageBoundaryPacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        if (ClientPacketHandler.validateBasics(msg, context)) {
            context.enqueueWork(() -> {
                Minecraft mc = Minecraft.getInstance();
                if (mc.level == null) {
                    return;
                }

                VillageBoundaryData data = new VillageBoundaryData(
                    msg.villageId,
                    msg.centerChunk,
                    msg.squadius
                );
                BoundaryDataStorage.getInstance().addVillage(mc.level.dimension(), data);
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
