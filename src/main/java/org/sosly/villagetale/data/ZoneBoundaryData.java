package org.sosly.villagetale.data;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;
import org.sosly.villagetale.VillageTale;
import org.sosly.villagetale.api.IZoneShape;
import org.sosly.villagetale.zone.shape.Box;
import org.sosly.villagetale.zone.shape.Cylinder;
import org.sosly.villagetale.zone.shape.Point;
import org.sosly.villagetale.zone.shape.Route;

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

    public IZoneShape toShape() {
        if (shapeType.equals(new ResourceLocation(VillageTale.MOD_ID, "box"))) {
            return new Box(bounds);
        }

        if (shapeType.equals(new ResourceLocation(VillageTale.MOD_ID, "cylinder"))) {
            if (center == null) {
                return null;
            }
            return new Cylinder(center, radius, height);
        }

        if (shapeType.equals(new ResourceLocation(VillageTale.MOD_ID, "point"))) {
            return new Point(BlockPos.containing(bounds.minX, bounds.minY, bounds.minZ));
        }

        if (shapeType.equals(new ResourceLocation(VillageTale.MOD_ID, "route"))) {
            if (waypoints == null) {
                return null;
            }
            Route route = new Route();
            for (BlockPos waypoint : waypoints) {
                route.addPoint(waypoint);
            }
            return route;
        }

        return null;
    }
}
