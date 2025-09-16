package org.sosly.villageworks.data;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.ChunkPos;

import java.util.UUID;

public class VillageData {
    
    private final UUID villageId;
    private final ChunkPos townHallPos;
    private final int squadius;
    private final String villageName;
    private BlockPos townHallBlockPos;
    
    public VillageData(UUID villageId, ChunkPos townHallPos, String villageName, int squadius) {
        this.villageId = villageId;
        this.townHallPos = townHallPos;
        this.villageName = villageName;
        this.squadius = squadius;
    }
    
    public UUID getVillageId() {
        return villageId;
    }
    
    public ChunkPos getTownHallPos() {
        return townHallPos;
    }
    
    public String getVillageName() {
        return villageName;
    }
    
    public int getSquadius() {
        return squadius;
    }
    
    public BlockPos getTownHallBlockPos() {
        return townHallBlockPos;
    }
    
    public void setTownHallBlockPos(BlockPos pos) {
        this.townHallBlockPos = pos;
    }
    
    public boolean containsChunk(ChunkPos pos) {
        if (pos == null) {
            return false;
        }
        
        int centerX = townHallPos.x;
        int centerZ = townHallPos.z;
        
        return pos.x >= centerX - squadius &&
               pos.x <= centerX + squadius &&
               pos.z >= centerZ - squadius &&
               pos.z <= centerZ + squadius;
    }
    
    public boolean overlaps(VillageData other) {
        return overlaps(other, 0);
    }
    
    public boolean overlaps(VillageData other, int minDistance) {
        if (other == null) {
            return false;
        }
        
        int thisMinX = townHallPos.x - squadius - minDistance;
        int thisMaxX = townHallPos.x + squadius + minDistance;
        int thisMinZ = townHallPos.z - squadius - minDistance;
        int thisMaxZ = townHallPos.z + squadius + minDistance;
        
        int otherMinX = other.townHallPos.x - other.squadius;
        int otherMaxX = other.townHallPos.x + other.squadius;
        int otherMinZ = other.townHallPos.z - other.squadius;
        int otherMaxZ = other.townHallPos.z + other.squadius;
        
        return thisMinX <= otherMaxX && thisMaxX >= otherMinX &&
               thisMinZ <= otherMaxZ && thisMaxZ >= otherMinZ;
    }
    
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putString("VillageId", villageId.toString());
        tag.putLong("TownHallPos", townHallPos.toLong());
        tag.putString("VillageName", villageName);
        tag.putInt("Squadius", squadius);
        if (townHallBlockPos != null) {
            tag.putLong("TownHallBlockPos", townHallBlockPos.asLong());
        }
        return tag;
    }
    
    public static VillageData deserializeNBT(CompoundTag tag) {
        if (!tag.contains("VillageId") || !tag.contains("TownHallPos") ||
            !tag.contains("VillageName") || !tag.contains("Squadius")) {
            return null;
        }
        
        try {
            UUID villageId = UUID.fromString(tag.getString("VillageId"));
            ChunkPos townHallPos = new ChunkPos(tag.getLong("TownHallPos"));
            String villageName = tag.getString("VillageName");
            int squadius = tag.getInt("Squadius");
            
            VillageData data = new VillageData(villageId, townHallPos, villageName, squadius);
            if (tag.contains("TownHallBlockPos")) {
                data.setTownHallBlockPos(BlockPos.of(tag.getLong("TownHallBlockPos")));
            }
            return data;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        
        VillageData other = (VillageData) obj;
        return villageId.equals(other.villageId);
    }
    
    @Override
    public int hashCode() {
        return villageId.hashCode();
    }
}