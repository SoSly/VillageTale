package org.sosly.villagetale.client.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@OnlyIn(Dist.CLIENT)
public class ClientZoneData {
    private final UUID zoneId;
    private final String name;
    private final ResourceLocation typeId;
    private final ResourceLocation shapeId;
    private final List<UUID> assignedVillagers;

    public ClientZoneData(UUID zoneId, String name, ResourceLocation typeId, ResourceLocation shapeId, List<UUID> assignedVillagers) {
        this.zoneId = zoneId;
        this.name = name;
        this.typeId = typeId;
        this.shapeId = shapeId;
        this.assignedVillagers = new ArrayList<>(assignedVillagers);
    }

    public UUID getZoneId() {
        return zoneId;
    }

    public String getName() {
        return name;
    }

    public ResourceLocation getTypeId() {
        return typeId;
    }

    public ResourceLocation getShapeId() {
        return shapeId;
    }

    public List<UUID> getAssignedVillagers() {
        return assignedVillagers;
    }

    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("ZoneId", zoneId);
        tag.putString("Name", name);
        tag.putString("TypeId", typeId.toString());
        tag.putString("ShapeId", shapeId.toString());
        tag.putInt("VillagerCount", assignedVillagers.size());
        return tag;
    }

    public static ClientZoneData deserializeNBT(CompoundTag tag) {
        UUID zoneId = tag.getUUID("ZoneId");
        String name = tag.getString("Name");
        ResourceLocation typeId = new ResourceLocation(tag.getString("TypeId"));
        ResourceLocation shapeId = new ResourceLocation(tag.getString("ShapeId"));
        int villagerCount = tag.getInt("VillagerCount");

        List<UUID> assignedVillagers = new ArrayList<>(villagerCount);
        return new ClientZoneData(zoneId, name, typeId, shapeId, assignedVillagers);
    }
}
