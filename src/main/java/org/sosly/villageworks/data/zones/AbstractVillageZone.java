package org.sosly.villageworks.data.zones;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import org.sosly.villageworks.api.data.IVillageZone;
import org.sosly.villageworks.api.data.ZoneType;

public abstract class AbstractVillageZone implements IVillageZone {
    private final UUID uuid;
    private final ZoneType type;
    private int id;
    private Level level;
    private String name;
    private final List<UUID> assignedVillagers;
    private final Map<BlockPos, Claim> claims;

    public AbstractVillageZone(UUID uuid, ZoneType type, String name, Level level) {
        this.uuid = Objects.requireNonNull(uuid, "UUID cannot be null");
        this.type = Objects.requireNonNull(type, "Zone type cannot be null");
        this.level = Objects.requireNonNull(level, "Level cannot be null");
        this.id = 0;
        this.name = name;
        this.assignedVillagers = new ArrayList<>();
        this.claims = new HashMap<>();
    }

    public AbstractVillageZone(UUID uuid, ZoneType type, String name) {
        this.uuid = Objects.requireNonNull(uuid, "UUID cannot be null");
        this.type = Objects.requireNonNull(type, "Zone type cannot be null");
        this.id = 0;
        this.name = name;
        this.level = null;
        this.assignedVillagers = new ArrayList<>();
        this.claims = new HashMap<>();
    }

    public AbstractVillageZone(UUID uuid, ZoneType type, int id, String name, Level level) {
        this.uuid = Objects.requireNonNull(uuid, "UUID cannot be null");
        this.type = Objects.requireNonNull(type, "Zone type cannot be null");
        this.level = Objects.requireNonNull(level, "Level cannot be null");
        this.id = id;
        this.name = name;
        this.assignedVillagers = new ArrayList<>();
        this.claims = new HashMap<>();
    }

    public AbstractVillageZone(UUID uuid, ZoneType type, int id, String name) {
        this.uuid = Objects.requireNonNull(uuid, "UUID cannot be null");
        this.type = Objects.requireNonNull(type, "Zone type cannot be null");
        this.id = id;
        this.name = name;
        this.level = null;
        this.assignedVillagers = new ArrayList<>();
        this.claims = new HashMap<>();
    }

    @Override
    public UUID getUUID() {
        return uuid;
    }

    @Override
    public String getName() {
        if (name != null) {
            return name;
        }

        String translationKey = "villageworks.zone." + type.name().toLowerCase();
        String zoneName = Component.translatable(translationKey).getString();
        return Component.translatable("villageworks.zone.numbered", zoneName, id).getString();
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public ZoneType getType() {
        return type;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @Override
    public Level getLevel() {
        return level;
    }

    public void setLevel(Level level) {
        this.level = Objects.requireNonNull(level, "Level cannot be null");
    }
    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putString("UUID", uuid.toString());
        tag.putByte("Type", (byte) type.ordinal());
        tag.putShort("Id", (short) id);
        if (name != null) {
            tag.putString("Name", name);
        }
        tag.putString("Level", level.dimension().location().toString());
        
        ListTag assignedVillagersTag = new ListTag();
        for (UUID villagerUUID : assignedVillagers) {
            assignedVillagersTag.add(StringTag.valueOf(villagerUUID.toString()));
        }
        tag.put("AssignedVillagers", assignedVillagersTag);
        
        CompoundTag claimsTag = new CompoundTag();
        for (Map.Entry<BlockPos, Claim> entry : claims.entrySet()) {
            BlockPos pos = entry.getKey();
            Claim claim = entry.getValue();
            String posKey = pos.getX() + "," + pos.getY() + "," + pos.getZ();
            claimsTag.put(posKey, claim.serializeNBT());
        }
        tag.put("Claims", claimsTag);
        
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        if (tag.contains("Name")) {
            this.name = tag.getString("Name");
        }
        
        if (tag.contains("AssignedVillagers")) {
            assignedVillagers.clear();
            ListTag assignedVillagersTag = tag.getList("AssignedVillagers", Tag.TAG_STRING);
            for (int i = 0; i < assignedVillagersTag.size(); i++) {
                try {
                    UUID villagerUUID = UUID.fromString(assignedVillagersTag.getString(i));
                    assignedVillagers.add(villagerUUID);
                } catch (IllegalArgumentException e) {
                    // Skip invalid UUIDs
                }
            }
        }
        
        if (tag.contains("Claims")) {
            claims.clear();
            CompoundTag claimsTag = tag.getCompound("Claims");
            for (String posKey : claimsTag.getAllKeys()) {
                try {
                    String[] coords = posKey.split(",");
                    if (coords.length == 3) {
                        int x = Integer.parseInt(coords[0]);
                        int y = Integer.parseInt(coords[1]);
                        int z = Integer.parseInt(coords[2]);
                        BlockPos pos = new BlockPos(x, y, z);
                        
                        Optional<Claim> claim = Claim.deserializeNBT(claimsTag.getCompound(posKey));
                        claim.ifPresent(c -> claims.put(pos, c));
                    }
                } catch (NumberFormatException e) {
                    // Skip invalid position keys
                }
            }
        }
    }
    
    @Override
    public List<UUID> getAssignedVillagers() {
        return new ArrayList<>(assignedVillagers);
    }
    
    @Override
    public void addAssignedVillager(UUID villagerUUID) {
        if (villagerUUID != null && !assignedVillagers.contains(villagerUUID)) {
            assignedVillagers.add(villagerUUID);
        }
    }
    
    @Override
    public boolean removeAssignedVillager(UUID villagerUUID) {
        return assignedVillagers.remove(villagerUUID);
    }
    
    @Override
    public Map<BlockPos, Optional<UUID>> getClaims(long currentTime) {
        claims.entrySet().removeIf(entry -> entry.getValue().isExpired(currentTime));
        
        Map<BlockPos, Optional<UUID>> result = new HashMap<>();
        
        Optional<List<BlockPos>> pois = getPOIs();
        if (!pois.isPresent()) {
            return result;
        }
        
        for (BlockPos pos : pois.get()) {
            Claim claim = claims.get(pos);
            if (claim != null && !claim.isExpired(currentTime)) {
                result.put(pos, Optional.of(claim.getVillagerUUID()));
            } else {
                result.put(pos, Optional.empty());
            }
        }
        
        return result;
    }
    
    @Override
    public boolean claim(BlockPos pos, UUID villagerUUID, int durationTicks, long currentTime) {
        if (pos == null || villagerUUID == null) {
            return false;
        }
        
        Optional<List<BlockPos>> pois = getPOIs();
        if (!pois.isPresent() || !pois.get().contains(pos)) {
            return false;
        }
        
        Claim existingClaim = claims.get(pos);
        if (existingClaim != null && !existingClaim.isExpired(currentTime)) {
            return false;
        }
        
        long expirationTime = currentTime + durationTicks;
        claims.put(pos, new Claim(villagerUUID, expirationTime));
        return true;
    }
    
    @Override
    public boolean release(BlockPos pos) {
        return claims.remove(pos) != null;
    }
}
