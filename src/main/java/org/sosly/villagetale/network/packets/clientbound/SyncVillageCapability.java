package org.sosly.villagetale.network.packets.clientbound;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import org.sosly.villagetale.VillageTale;
import org.sosly.villagetale.api.capability.IVillageCapability;
import org.sosly.villagetale.api.IVillageZone;
import org.sosly.villagetale.client.VillageDataManager;
import org.sosly.villagetale.client.data.ClientVillageData;
import org.sosly.villagetale.client.data.ClientZoneData;
import org.sosly.villagetale.network.BasePacket;
import org.sosly.villagetale.network.ClientPacketHandler;
import org.sosly.villagetale.network.NetworkHandler;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class SyncVillageCapability extends BasePacket {
    private final CompoundTag villageData;

    private SyncVillageCapability(CompoundTag villageData) {
        this.villageData = villageData;
    }

    public static void send(ServerPlayer player, IVillageCapability capability, MinecraftServer server) {
        Map<UUID, String> playerNames = new HashMap<>();
        for (Map.Entry<UUID, IVillageCapability.Permission> entry : capability.getPlayerPermissions().entrySet()) {
            GameProfile profile = server.getProfileCache().get(entry.getKey()).orElse(null);
            if (profile != null) {
                playerNames.put(entry.getKey(), profile.getName());
            }
        }

        List<ClientZoneData> zones = capability.getZones().stream()
                .map(zone -> new ClientZoneData(
                        zone.getUUID(),
                        zone.getName(),
                        zone.getType().getID(),
                        zone.getShape().getID(),
                        zone.getAssignedVillagers()
                ))
                .collect(Collectors.toList());

        ClientVillageData clientData = new ClientVillageData(
                capability.getUUID(),
                capability.getName(),
                capability.getChunk().getPos(),
                3,
                capability.getVillagerUUIDs(),
                zones,
                capability.getPlayerPermissions(),
                playerNames
        );

        SyncVillageCapability packet = new SyncVillageCapability(clientData.serializeNBT());
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
            ClientVillageData data = ClientVillageData.deserializeNBT(msg.villageData);
            VillageDataManager.getInstance().updateVillageData(data.getVillageId(), data);
        });
    }
}
