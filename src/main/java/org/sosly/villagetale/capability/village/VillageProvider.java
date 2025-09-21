package org.sosly.villagetale.capability.village;

import java.util.UUID;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.sosly.villagetale.api.IVillageZone;
import org.sosly.villagetale.api.capability.IVillageCapability;
import org.sosly.villagetale.capability.Capabilities;
import org.sosly.villagetale.zone.Zone;

public class VillageProvider implements ICapabilitySerializable<CompoundTag> {
    private final VillageCapability capability;
    private final LazyOptional<IVillageCapability> holder;

    public VillageProvider() {
        this.capability = new VillageCapability();
        this.holder = LazyOptional.of(() -> capability);
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        if (cap != Capabilities.VILLAGE_CAPABILITY){
            return LazyOptional.empty();
        }

        return holder.cast();
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        if (capability == null || capability.getUUID() == null) {
            return tag;
        }

        tag.putUUID("village", capability.getUUID());
        tag.putString("name", capability.getName());

        ListTag zones = new ListTag();
        for (IVillageZone zone : capability.getZones()) {
            zones.add(zone.serializeNBT());
        }
        tag.put("zones", zones);

        ListTag villagers = new ListTag();
        for (UUID uuid : capability.getVillagerUUIDs()) {
            CompoundTag villager = new CompoundTag();
            villager.putUUID("villager", uuid);
            villagers.add(villager);
        }
        tag.put("villagers", villagers);

        ListTag players = new ListTag();
        capability.getPlayerPermissions().forEach((key, value) -> {
            CompoundTag player = new CompoundTag();
            player.putUUID("player", key);
            player.putString("permission", value.toString());
            players.add(player);
        });
        tag.put("players", players);

        capability.getChunk().setUnsaved(false);
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        if (!tag.contains("village")) {
            return;
        }

        capability.setUUID(tag.getUUID("village"));
        capability.setName(tag.getString("name"));

        if (tag.contains("zones")) {
            ListTag zones = tag.getList("zones", 10);
            for (int i = 0; i < zones.size(); i++) {
                IVillageZone zone = new Zone(null);
                zone.deserializeNBT(capability, zones.getCompound(i));
                capability.addZone(zone);
            }
        }

        if (tag.contains("villagers")) {
            ListTag villagers = tag.getList("villagers", 10);
            for (int i = 0; i < villagers.size(); i++) {
                UUID uuid = villagers.getCompound(i).getUUID("villager");
                capability.addVillagerByUUID(uuid);
            }
        }

        if (tag.contains("players")) {
            ListTag players = tag.getList("players", 10);
            for (int i = 0; i < players.size(); i++) {
                UUID uuid = players.getCompound(i).getUUID("player");
                IVillageCapability.Permission permission = IVillageCapability.Permission
                    .fromString(players.getCompound(i).getString("permission"));
                capability.setPlayerPermission(uuid, permission);
            }
        }
    }
}
