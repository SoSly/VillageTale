package org.sosly.villagetale.network.packets.clientbound;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import org.sosly.villagetale.VillageTale;
import org.sosly.villagetale.api.capability.IVillageCapability;
import org.sosly.villagetale.client.VillageDataManager;
import org.sosly.villagetale.network.BasePacket;
import org.sosly.villagetale.network.ClientPacketHandler;
import org.sosly.villagetale.network.NetworkHandler;

import java.util.function.Supplier;
import org.sosly.villagetale.capability.village.VillageCapability;

public class SyncVillageCapability extends BasePacket {
    private final CompoundTag villageData;

    private SyncVillageCapability(CompoundTag villageData) {
        this.villageData = villageData;
    }

    public static void send(ServerPlayer player, IVillageCapability capability, MinecraftServer server) {
        if (!(capability instanceof VillageCapability)) {
            return;
        }

        VillageCapability villageCapability =
            (VillageCapability) capability;

        SyncVillageCapability packet = new SyncVillageCapability(villageCapability.serializeNBT());
        NetworkHandler.CHANNEL.sendTo(packet, player.connection.connection, NetworkDirection.PLAY_TO_CLIENT);
    }

    public static void encode(SyncVillageCapability msg, FriendlyByteBuf buffer) {
        buffer.writeNbt(msg.villageData);
    }

    public static SyncVillageCapability decode(FriendlyByteBuf buffer) {
        SyncVillageCapability msg;

        try {
            CompoundTag villageData = buffer.readNbt();
            msg = new SyncVillageCapability(villageData);
        } catch (Exception err) {
            VillageTale.LOGGER.error("Exception while reading SyncVillageCapability: {}", err.toString());
            return null;
        }

        msg.messageIsValid = true;
        return msg;
    }

    public static void handle(SyncVillageCapability msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        if (!ClientPacketHandler.validateBasics(msg, context)) {
            return;
        }

        context.enqueueWork(() -> {
            VillageCapability capability =
                new VillageCapability();
            capability.deserializeNBT(msg.villageData);
            VillageDataManager.getInstance().updateVillageData(capability.getUUID(), capability);
        }).whenComplete((r, e) -> {
            if (e != null) {
                throw new RuntimeException("Failed to handle SyncVillageCapability", e);
            }
        });
    }
}
