package org.sosly.villagetale.capability.village;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.sosly.villagetale.api.capability.IVillageCapability;
import org.sosly.villagetale.capability.Capabilities;
import org.sosly.villagetale.data.zones.ZoneFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;

public class VillageProvider implements ICapabilitySerializable<CompoundTag> {

    private final VillageCapability capability;
    private final LazyOptional<IVillageCapability> lazyOptional;

    public VillageProvider() {
        this.capability = new VillageCapability();
        this.lazyOptional = LazyOptional.of(() -> capability);
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        if (cap == Capabilities.VILLAGE_CAPABILITY) {
            return lazyOptional.cast();
        }
        return LazyOptional.empty();
    }

    @Override
    public CompoundTag serializeNBT() {
        if (capability.getVillageId() == null) {
            return new CompoundTag();
        }

        CompoundTag tag = new CompoundTag();

        tag.putString("VillageId", capability.getVillageId().toString());

        ListTag villagerList = new ListTag();
        for (UUID villagerId : capability.getVillagerIds()) {
            villagerList.add(StringTag.valueOf(villagerId.toString()));
        }
        tag.put("VillagerIds", villagerList);

        CompoundTag permissionsTag = new CompoundTag();
        for (var entry : capability.getPlayerPermissions().entrySet()) {
            permissionsTag.putByte(entry.getKey().toString(), (byte) entry.getValue().ordinal());
        }
        tag.put("PlayerPermissions", permissionsTag);

        ListTag zoneList = new ListTag();
        for (var zone : capability.getZones()) {
            zoneList.add(zone.serializeNBT());
        }
        tag.put("Zones", zoneList);

        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        if (!tag.hasUUID("VillageId") && !tag.contains("VillageId", Tag.TAG_STRING)) {
            return;
        }

        UUID villageId = UUID.fromString(tag.getString("VillageId"));

        capability.initializeVillage(villageId);

        // Deserialize villager IDs
        if (tag.contains("VillagerIds", Tag.TAG_LIST)) {
            ListTag villagerList = tag.getList("VillagerIds", Tag.TAG_STRING);
            for (int i = 0; i < villagerList.size(); i++) {
                try {
                    UUID villagerId = UUID.fromString(villagerList.getString(i));
                    capability.assignVillager(villagerId);
                } catch (IllegalArgumentException e) {
                    // Skip invalid UUIDs
                }
            }
        }

        if (tag.contains("PlayerPermissions", Tag.TAG_COMPOUND)) {
            CompoundTag permissionsTag = tag.getCompound("PlayerPermissions");
            for (String playerIdStr : permissionsTag.getAllKeys()) {
                try {
                    UUID playerId = UUID.fromString(playerIdStr);
                    byte permissionOrdinal = permissionsTag.getByte(playerIdStr);
                    if (permissionOrdinal >= 0 && permissionOrdinal < IVillageCapability.Permission.values().length) {
                        IVillageCapability.Permission permission = IVillageCapability.Permission.values()[permissionOrdinal];
                        capability.setPlayerPermission(playerId, permission);
                    }
                } catch (IllegalArgumentException e) {
                    // Skip invalid UUIDs
                }
            }
        }

        // Deserialize zones
        if (tag.contains("Zones", Tag.TAG_LIST)) {
            ListTag zoneList = tag.getList("Zones", Tag.TAG_COMPOUND);
            for (int i = 0; i < zoneList.size(); i++) {
                CompoundTag zoneTag = zoneList.getCompound(i);
                var zone = ZoneFactory.deserializeFromNBT(zoneTag);
                if (zone != null) {
                    capability.addExistingZone(zone);
                }
            }
        }
    }

    public void invalidate() {
        lazyOptional.invalidate();
    }
}
