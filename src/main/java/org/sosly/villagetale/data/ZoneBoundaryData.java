package org.sosly.villagetale.data;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;

import java.util.UUID;

public class ZoneBoundaryData {
    private final UUID zoneId;
    private final UUID villageId;
    private final ResourceLocation shapeType;
    private final AABB bounds;

    public ZoneBoundaryData(UUID zoneId, UUID villageId, ResourceLocation shapeType, AABB bounds) {
        this.zoneId = zoneId;
        this.villageId = villageId;
        this.shapeType = shapeType;
        this.bounds = bounds;
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
