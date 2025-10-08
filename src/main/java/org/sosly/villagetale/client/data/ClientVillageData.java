package org.sosly.villagetale.client.data;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.ChunkPos;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.sosly.villagetale.api.capability.IVillageCapability;
import org.sosly.villagetale.api.IVillageZone;

import java.util.*;
import java.util.stream.Collectors;

@OnlyIn(Dist.CLIENT)
public class ClientVillageData {
    private final UUID villageId;
    private final String villageName;
    private final ChunkPos startingChunk;
    private final int squadius;
    private final Set<UUID> villagerUUIDs;
    private final List<ClientZoneData> zones;
    private final Map<UUID, IVillageCapability.Permission> playerPermissions;
    private final Map<UUID, String> playerNames;

    public ClientVillageData(
            UUID villageId,
            String villageName,
            ChunkPos startingChunk,
            int squadius,
            Set<UUID> villagerUUIDs,
            List<ClientZoneData> zones,
            Map<UUID, IVillageCapability.Permission> playerPermissions,
            Map<UUID, String> playerNames
    ) {
        this.villageId = villageId;
        this.villageName = villageName;
        this.startingChunk = startingChunk;
        this.squadius = squadius;
        this.villagerUUIDs = new HashSet<>(villagerUUIDs);
        this.zones = new ArrayList<>(zones);
        this.playerPermissions = new HashMap<>(playerPermissions);
        this.playerNames = new HashMap<>(playerNames);
    }

    public UUID getVillageId() {
        return villageId;
    }

    public String getVillageName() {
        return villageName;
    }

    public ChunkPos getStartingChunk() {
        return startingChunk;
    }

    public int getSquadius() {
        return squadius;
    }

    public Set<UUID> getVillagerUUIDs() {
        return Collections.unmodifiableSet(villagerUUIDs);
    }

    public List<ClientZoneData> getZones() {
        return Collections.unmodifiableList(zones);
    }

    public Map<UUID, IVillageCapability.Permission> getPlayerPermissions() {
        return Collections.unmodifiableMap(playerPermissions);
    }

    public List<String> getOwnerNames() {
        return playerPermissions.entrySet().stream()
                .filter(entry -> entry.getValue() == IVillageCapability.Permission.OWNER)
                .map(entry -> playerNames.getOrDefault(entry.getKey(), "Unknown"))
                .sorted()
                .collect(Collectors.toList());
    }

    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("VillageId", villageId);
        tag.putString("VillageName", villageName);
        tag.putLong("StartingChunk", startingChunk.toLong());
        tag.putInt("Squadius", squadius);

        ListTag villagerList = new ListTag();
        for (UUID uuid : villagerUUIDs) {
            CompoundTag villagerTag = new CompoundTag();
            villagerTag.putUUID("UUID", uuid);
            villagerList.add(villagerTag);
        }
        tag.put("Villagers", villagerList);

        ListTag zoneList = new ListTag();
        for (ClientZoneData zone : zones) {
            zoneList.add(zone.serializeNBT());
        }
        tag.put("Zones", zoneList);

        ListTag permissionList = new ListTag();
        for (Map.Entry<UUID, IVillageCapability.Permission> entry : playerPermissions.entrySet()) {
            CompoundTag permTag = new CompoundTag();
            permTag.putUUID("Player", entry.getKey());
            permTag.putString("Permission", entry.getValue().toString());
            permTag.putString("Name", playerNames.getOrDefault(entry.getKey(), "Unknown"));
            permissionList.add(permTag);
        }
        tag.put("Permissions", permissionList);

        return tag;
    }

    public static ClientVillageData deserializeNBT(CompoundTag tag) {
        UUID villageId = tag.getUUID("VillageId");
        String villageName = tag.getString("VillageName");
        ChunkPos startingChunk = new ChunkPos(tag.getLong("StartingChunk"));
        int squadius = tag.getInt("Squadius");

        Set<UUID> villagerUUIDs = new HashSet<>();
        ListTag villagerList = tag.getList("Villagers", Tag.TAG_COMPOUND);
        for (int i = 0; i < villagerList.size(); i++) {
            CompoundTag villagerTag = villagerList.getCompound(i);
            villagerUUIDs.add(villagerTag.getUUID("UUID"));
        }

        List<ClientZoneData> zones = new ArrayList<>();
        ListTag zoneList = tag.getList("Zones", Tag.TAG_COMPOUND);
        for (int i = 0; i < zoneList.size(); i++) {
            zones.add(ClientZoneData.deserializeNBT(zoneList.getCompound(i)));
        }

        Map<UUID, IVillageCapability.Permission> playerPermissions = new HashMap<>();
        Map<UUID, String> playerNames = new HashMap<>();
        ListTag permissionList = tag.getList("Permissions", Tag.TAG_COMPOUND);
        for (int i = 0; i < permissionList.size(); i++) {
            CompoundTag permTag = permissionList.getCompound(i);
            UUID playerId = permTag.getUUID("Player");
            IVillageCapability.Permission permission = IVillageCapability.Permission.fromString(permTag.getString("Permission"));
            String name = permTag.getString("Name");
            playerPermissions.put(playerId, permission);
            playerNames.put(playerId, name);
        }

        return new ClientVillageData(villageId, villageName, startingChunk, squadius, villagerUUIDs, zones, playerPermissions, playerNames);
    }
}
