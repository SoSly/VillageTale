package org.sosly.villagetale.network.packets.clientbound;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;
import org.sosly.villagetale.VillageTale;
import org.sosly.villagetale.api.IZoneShape;
import org.sosly.villagetale.client.BoundaryDataStorage;
import org.sosly.villagetale.data.ZoneBoundaryData;
import org.sosly.villagetale.network.BasePacket;
import org.sosly.villagetale.network.ClientPacketHandler;
import org.sosly.villagetale.network.NetworkHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

public class ZoneBoundary extends BasePacket {
    private final UUID zoneId;
    private final UUID villageId;
    private final ResourceLocation shapeType;
    private final AABB bounds;
    private final BlockPos center;
    private final int radius;
    private final int height;
    private final List<BlockPos> waypoints;

    public ZoneBoundary(UUID zoneId, UUID villageId, ResourceLocation shapeType, AABB bounds) {
        this(zoneId, villageId, shapeType, bounds, null, 0, 0, null);
    }

    public ZoneBoundary(UUID zoneId, UUID villageId, ResourceLocation shapeType, AABB bounds,
                        BlockPos center, int radius, int height, List<BlockPos> waypoints) {
        this.zoneId = zoneId;
        this.villageId = villageId;
        this.shapeType = shapeType;
        this.bounds = bounds;
        this.center = center;
        this.radius = radius;
        this.height = height;
        this.waypoints = waypoints;
    }

    public static void sendToPlayer(ServerPlayer player, UUID zoneId, UUID villageId, IZoneShape shape) {
        if (shape == null) {
            return;
        }
        ZoneBoundary packet = shape.createBoundaryPacket(zoneId, villageId);
        if (packet == null) {
            return;
        }
        NetworkHandler.CHANNEL.send(
            PacketDistributor.PLAYER.with(() -> player),
            packet
        );
    }

    public static void sendToDimension(ResourceKey<Level> dimension, UUID zoneId, UUID villageId, IZoneShape shape) {
        if (shape == null) {
            return;
        }
        ZoneBoundary packet = shape.createBoundaryPacket(zoneId, villageId);
        if (packet == null) {
            return;
        }
        NetworkHandler.CHANNEL.send(
            PacketDistributor.DIMENSION.with(() -> dimension),
            packet
        );
    }

    public static void encode(ZoneBoundary msg, FriendlyByteBuf buffer) {
        buffer.writeUUID(msg.zoneId);
        buffer.writeUUID(msg.villageId);
        buffer.writeResourceLocation(msg.shapeType);
        buffer.writeDouble(msg.bounds.minX);
        buffer.writeDouble(msg.bounds.minY);
        buffer.writeDouble(msg.bounds.minZ);
        buffer.writeDouble(msg.bounds.maxX);
        buffer.writeDouble(msg.bounds.maxY);
        buffer.writeDouble(msg.bounds.maxZ);

        buffer.writeBoolean(msg.center != null);
        if (msg.center != null) {
            buffer.writeBlockPos(msg.center);
        }

        buffer.writeInt(msg.radius);
        buffer.writeInt(msg.height);

        buffer.writeBoolean(msg.waypoints != null);
        if (msg.waypoints != null) {
            buffer.writeInt(msg.waypoints.size());
            for (BlockPos waypoint : msg.waypoints) {
                buffer.writeBlockPos(waypoint);
            }
        }
    }

    public static ZoneBoundary decode(FriendlyByteBuf buffer) {
        ZoneBoundary msg;

        try {
            UUID zoneId = buffer.readUUID();
            UUID villageId = buffer.readUUID();
            ResourceLocation shapeType = buffer.readResourceLocation();
            double minX = buffer.readDouble();
            double minY = buffer.readDouble();
            double minZ = buffer.readDouble();
            double maxX = buffer.readDouble();
            double maxY = buffer.readDouble();
            double maxZ = buffer.readDouble();
            AABB bounds = new AABB(minX, minY, minZ, maxX, maxY, maxZ);

            BlockPos center = null;
            if (buffer.readBoolean()) {
                center = buffer.readBlockPos();
            }

            int radius = buffer.readInt();
            int height = buffer.readInt();

            List<BlockPos> waypoints = null;
            if (buffer.readBoolean()) {
                int size = buffer.readInt();
                waypoints = new ArrayList<>(size);
                for (int i = 0; i < size; i++) {
                    waypoints.add(buffer.readBlockPos());
                }
            }

            msg = new ZoneBoundary(zoneId, villageId, shapeType, bounds, center, radius, height, waypoints);
        } catch (IndexOutOfBoundsException | IllegalArgumentException err) {
            VillageTale.LOGGER.error("Exception while reading ZoneBoundary: {}", err.toString());
            return null;
        }

        msg.messageIsValid = true;
        return msg;
    }

    public static void handle(ZoneBoundary msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        if (ClientPacketHandler.validateBasics(msg, context)) {
            context.enqueueWork(() -> {
                Minecraft mc = Minecraft.getInstance();
                if (mc.level == null) {
                    return;
                }

                ZoneBoundaryData data = new ZoneBoundaryData(
                    msg.zoneId,
                    msg.villageId,
                    msg.shapeType,
                    msg.bounds,
                    msg.center,
                    msg.radius,
                    msg.height,
                    msg.waypoints
                );
                BoundaryDataStorage.getInstance().addZone(mc.level.dimension(), data);
            });
        }
    }

    public UUID getZoneId() {
        return zoneId;
    }

    public UUID getVillageId() {
        return villageId;
    }

    public ResourceLocation getShapeType() {
        return shapeType;
    }

    public AABB getBounds() {
        return bounds;
    }

    public BlockPos getCenter() {
        return center;
    }

    public int getRadius() {
        return radius;
    }

    public int getHeight() {
        return height;
    }

    public List<BlockPos> getWaypoints() {
        return waypoints;
    }
}
