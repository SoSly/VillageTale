package org.sosly.villagetale.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public class ZoneBoundaryPacket {
    private final UUID zoneId;
    private final UUID villageId;
    private final ResourceLocation shapeType;
    private final AABB bounds;

    public ZoneBoundaryPacket(UUID zoneId, UUID villageId, ResourceLocation shapeType, AABB bounds) {
        this.zoneId = zoneId;
        this.villageId = villageId;
        this.shapeType = shapeType;
        this.bounds = bounds;
    }

    public static void encode(ZoneBoundaryPacket msg, FriendlyByteBuf buffer) {
        buffer.writeUUID(msg.zoneId);
        buffer.writeUUID(msg.villageId);
        buffer.writeResourceLocation(msg.shapeType);
        buffer.writeDouble(msg.bounds.minX);
        buffer.writeDouble(msg.bounds.minY);
        buffer.writeDouble(msg.bounds.minZ);
        buffer.writeDouble(msg.bounds.maxX);
        buffer.writeDouble(msg.bounds.maxY);
        buffer.writeDouble(msg.bounds.maxZ);
    }

    public static ZoneBoundaryPacket decode(FriendlyByteBuf buffer) {
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
        return new ZoneBoundaryPacket(zoneId, villageId, shapeType, bounds);
    }

    public static void handle(ZoneBoundaryPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> ClientPacketHandler.handleZoneBoundary(msg));
        ctx.get().setPacketHandled(true);
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
}
