package org.sosly.villagetale.data;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.UUID;

public class ZoneBoundaryData {
    private final UUID zoneId;
    private final UUID villageId;
    private final ResourceLocation shapeType;
    private final AABB bounds;
    private final BlockPos center;
    private final int radius;
    private final int height;
    private final List<BlockPos> waypoints;

    public ZoneBoundaryData(UUID zoneId, UUID villageId, ResourceLocation shapeType, AABB bounds) {
        this(zoneId, villageId, shapeType, bounds, null, 0, 0, null);
    }

    public ZoneBoundaryData(UUID zoneId, UUID villageId, ResourceLocation shapeType, AABB bounds,
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
