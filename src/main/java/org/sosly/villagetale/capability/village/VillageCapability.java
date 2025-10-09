package org.sosly.villagetale.capability.village;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import org.sosly.villagetale.api.IVillageZone;
import org.sosly.villagetale.api.capability.IVillageCapability;
import org.sosly.villagetale.network.packets.clientbound.ZoneBoundary;
import org.sosly.villagetale.zone.Zone;

public class VillageCapability implements IVillageCapability {

    @Nullable
    private UUID id;

    private ResourceKey<Level> dimension;
    private Map<ResourceLocation, Integer> ordinals = Collections.synchronizedMap(new HashMap<>());
    private String name;
    private Map<UUID, IVillageZone> zones = Collections.synchronizedMap(new HashMap<>());
    private Set<UUID> villagers = Collections.synchronizedSet(new HashSet<>());
    private Map<UUID, Permission> players = Collections.synchronizedMap(new HashMap<>());
    private WeakReference<LevelChunk> chunk;

    @Override
    public UUID getUUID() {
        return id;
    }

    @Override
    public void setUUID(UUID uuid) {
        this.id = uuid;
    }

    @Override
    public LevelChunk getChunk() {
        return chunk == null ? null : chunk.get();
    }

    @Override
    public void setChunk(LevelChunk chunk) {
        this.chunk = new WeakReference<>(chunk);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
        markDirty();
    }

    @Override
    public List<IVillageZone> getZones() {
        return zones.values().stream().toList();
    }

    @Override
    public void addZone(IVillageZone zone) {
        zones.put(zone.getUUID(), zone);
        markDirty();

        if (chunk == null || chunk.get() == null) {
            return;
        }

        if (chunk.get().getLevel().isClientSide) {
            return;
        }

        ZoneBoundary.sendToDimension(chunk.get().getLevel().dimension(), zone.getUUID(), this.id, zone.getShape());
    }

    @Override
    public boolean removeZone(UUID zoneId) {
        if (!zones.containsKey(zoneId)) {
            return false;
        }

        zones.remove(zoneId);
        markDirty();
        return true;
    }

    @Override
    public IVillageZone getZoneAt(BlockPos pos) {
        return zones.values().stream()
                .filter(zone -> zone.containsPosition(pos))
                .findFirst()
                .orElse(null);
    }

    @Override
    public Set<UUID> getVillagerUUIDs() {
        return villagers;
    }

    @Override
    public void addVillagerByUUID(UUID villager) {
        if (villagers.contains(villager)) {
            return;
        }

        villagers.add(villager);
        markDirty();
    }

    @Override
    public boolean removeVillagerByUUID(UUID villager) {
        if (!villagers.contains(villager)) {
            return false;
        }

        villagers.remove(villager);
        markDirty();
        return true;
    }

    @Override
    public Map<UUID, Permission> getPlayerPermissions() {
        return players;
    }

    @Override
    public void setPlayerPermission(UUID player, Permission permission) {
        players.put(player, permission);
        markDirty();
    }

    @Override
    public boolean hasPermission(UUID player, Permission required) {
        return players.containsKey(player) && players.get(player).equals(required);
    }

    private void markDirty() {
        LevelChunk chunk = this.chunk.get();
        if (chunk == null) {
            return;
        }

        chunk.setUnsaved(true);
    }

    public void destroy() {
        markDirty();
        this.id = null;
        this.dimension = null;
        this.ordinals = null;
        this.zones = null;
        this.villagers = null;
        this.players = null;
        this.chunk = null;
    }

    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        if (id == null) {
            return tag;
        }

        tag.putUUID("village", id);
        tag.putString("name", name);

        ListTag zoneList = new ListTag();
        for (IVillageZone zone : zones.values()) {
            zoneList.add(zone.serializeNBT());
        }
        tag.put("zones", zoneList);

        ListTag villagerList = new ListTag();
        for (UUID uuid : villagers) {
            CompoundTag villagerTag = new CompoundTag();
            villagerTag.putUUID("villager", uuid);
            villagerList.add(villagerTag);
        }
        tag.put("villagers", villagerList);

        ListTag playerList = new ListTag();
        players.forEach((key, value) -> {
            CompoundTag playerTag = new CompoundTag();
            playerTag.putUUID("player", key);
            playerTag.putString("permission", value.toString());
            playerList.add(playerTag);
        });
        tag.put("players", playerList);

        return tag;
    }

    public void deserializeNBT(CompoundTag tag) {
        if (!tag.contains("village")) {
            return;
        }

        id = tag.getUUID("village");
        name = tag.getString("name");

        if (tag.contains("zones")) {
            ListTag zoneList = tag.getList("zones", 10);
            for (int i = 0; i < zoneList.size(); i++) {
                IVillageZone zone = new Zone(null);
                zone.deserializeNBT(this, zoneList.getCompound(i));
                zones.put(zone.getUUID(), zone);
            }
        }

        if (tag.contains("villagers")) {
            ListTag villagerList = tag.getList("villagers", 10);
            for (int i = 0; i < villagerList.size(); i++) {
                UUID uuid = villagerList.getCompound(i).getUUID("villager");
                villagers.add(uuid);
            }
        }

        if (tag.contains("players")) {
            ListTag playerList = tag.getList("players", 10);
            for (int i = 0; i < playerList.size(); i++) {
                UUID uuid = playerList.getCompound(i).getUUID("player");
                Permission permission = Permission.fromString(playerList.getCompound(i).getString("permission"));
                players.put(uuid, permission);
            }
        }
    }
}
