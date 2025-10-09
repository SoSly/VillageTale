package org.sosly.villagetale.network.packets.serverbound;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraftforge.network.NetworkEvent;
import org.sosly.villagetale.VillageTale;
import org.sosly.villagetale.api.IVillageZone;
import org.sosly.villagetale.api.capability.IVillageCapability;
import org.sosly.villagetale.api.capability.IVillagesCapability;
import org.sosly.villagetale.capability.Capabilities;
import org.sosly.villagetale.data.VillageInfo;
import org.sosly.villagetale.network.BasePacket;
import org.sosly.villagetale.network.NetworkHandler;
import org.sosly.villagetale.network.ServerPacketHandler;
import org.sosly.villagetale.network.packets.clientbound.SyncVillageCapability;

public class UpdateZoneFilters extends BasePacket {
    private final UUID villageId;
    private final UUID zoneId;
    private final FilterType filterType;
    private final List<ResourceLocation> filters;

    public enum FilterType {
        ITEM,
        ENTITY
    }

    private UpdateZoneFilters(UUID villageId, UUID zoneId, FilterType filterType, List<ResourceLocation> filters) {
        this.villageId = villageId;
        this.zoneId = zoneId;
        this.filterType = filterType;
        this.filters = filters;
    }

    public static void send(UUID villageId, UUID zoneId, FilterType filterType, List<ResourceLocation> filters) {
        UpdateZoneFilters packet = new UpdateZoneFilters(villageId, zoneId, filterType, filters);
        NetworkHandler.CHANNEL.sendToServer(packet);
    }

    public static void encode(UpdateZoneFilters msg, FriendlyByteBuf buffer) {
        buffer.writeUUID(msg.villageId);
        buffer.writeUUID(msg.zoneId);
        buffer.writeEnum(msg.filterType);
        buffer.writeInt(msg.filters.size());
        for (ResourceLocation filter : msg.filters) {
            buffer.writeResourceLocation(filter);
        }
    }

    public static UpdateZoneFilters decode(FriendlyByteBuf buffer) {
        UpdateZoneFilters msg;

        try {
            UUID villageId = buffer.readUUID();
            UUID zoneId = buffer.readUUID();
            FilterType filterType = buffer.readEnum(FilterType.class);
            int size = buffer.readInt();
            List<ResourceLocation> filters = new ArrayList<>();
            for (int i = 0; i < size; i++) {
                filters.add(buffer.readResourceLocation());
            }
            msg = new UpdateZoneFilters(villageId, zoneId, filterType, filters);
        } catch (IndexOutOfBoundsException | IllegalArgumentException err) {
            VillageTale.LOGGER.error("Exception while reading UpdateZoneFilters: {}", err.toString());
            return null;
        }

        msg.messageIsValid = true;
        return msg;
    }

    public static void handle(UpdateZoneFilters msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        if (!ServerPacketHandler.validateBasics(msg, context)) {
            return;
        }

        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }

            ServerLevel level = player.serverLevel();
            IVillagesCapability villagesCapability = level.getCapability(Capabilities.VILLAGES_CAPABILITY).orElse(null);
            if (villagesCapability == null) {
                player.sendSystemMessage(Component.literal("Failed to update zone filters: capability not found"));
                return;
            }

            VillageInfo village = villagesCapability.getVillageById(msg.villageId);
            if (village == null) {
                player.sendSystemMessage(Component.literal("Village not found"));
                return;
            }

            ChunkPos villageChunk = village.getVillageStartingChunk();
            IVillageCapability villageCapability = level.getChunk(villageChunk.x, villageChunk.z)
                    .getCapability(Capabilities.VILLAGE_CAPABILITY).orElse(null);

            if (villageCapability == null) {
                player.sendSystemMessage(Component.literal("Failed to update zone filters: capability not found"));
                return;
            }

            if (!villageCapability.hasPermission(player.getUUID(), IVillageCapability.Permission.OWNER)) {
                player.sendSystemMessage(Component.literal("You do not have permission to modify zones in this village"));
                return;
            }

            IVillageZone zone = villageCapability.getZones().stream()
                    .filter(z -> z.getUUID().equals(msg.zoneId))
                    .findFirst()
                    .orElse(null);

            if (zone == null) {
                player.sendSystemMessage(Component.literal("Zone not found"));
                return;
            }

            if (msg.filterType == FilterType.ITEM) {
                msg.setItemFilters(player, zone);
            } else {
                msg.setEntityFilters(player, zone);
            }

            SyncVillageCapability.send(player, villageCapability, player.server);
        });
    }

    private void setItemFilters(ServerPlayer player, IVillageZone zone) {
        if (!zone.getType().supportsItemFilters()) {
            player.sendSystemMessage(Component.literal("This zone type does not support item filters"));
            return;
        }

        List<ItemStack> itemFilters = new ArrayList<>();
        for (ResourceLocation itemId : filters) {
            itemFilters.add(new ItemStack(BuiltInRegistries.ITEM.get(itemId)));
        }

        zone.setFilter(itemFilters);
    }

    private void setEntityFilters(ServerPlayer player, IVillageZone zone) {
        if (!zone.getType().supportsEntityFilters()) {
            player.sendSystemMessage(Component.literal("This zone type does not support entity filters"));
            return;
        }

        Set<ResourceLocation> entityFilters = new HashSet<>(filters);
        zone.setEntityTypeFilter(entityFilters);
    }

    public UUID getVillageId() {
        return villageId;
    }

    public UUID getZoneId() {
        return zoneId;
    }

    public FilterType getFilterType() {
        return filterType;
    }

    public List<ResourceLocation> getFilters() {
        return filters;
    }
}
