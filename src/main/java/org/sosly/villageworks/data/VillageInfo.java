package org.sosly.villageworks.data;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.ChunkPos;

import java.util.UUID;

public class VillageInfo {
    
    private final UUID villageId;
    private BlockPos townHallPos;
    private final ChunkPos villageStartingChunk;
    private final int squadius;
    private final String villageName;
    
    public VillageInfo(UUID villageId, BlockPos townHallPos, ChunkPos villageStartingChunk, String villageName, int squadius) {
        this.villageId = villageId;
        this.townHallPos = townHallPos;
        this.villageStartingChunk = villageStartingChunk;
        this.villageName = villageName;
        this.squadius = squadius;
    }
    
    public UUID getVillageId() {
        return villageId;
    }
    
    public BlockPos getTownHallPos() {
        return townHallPos;
    }
    
    public void setTownHallPos(BlockPos townHallPos) {
        this.townHallPos = townHallPos;
    }
    
    public ChunkPos getVillageStartingChunk() {
        return villageStartingChunk;
    }
    
    public String getVillageName() {
        return villageName;
    }
    
    public int getSquadius() {
        return squadius;
    }
    
    
    public boolean containsChunk(ChunkPos pos) {
        if (pos == null) {
            return false;
        }
        
        ChunkPos centerChunk = townHallPos != null ? new ChunkPos(townHallPos) : villageStartingChunk;
        int centerX = centerChunk.x;
        int centerZ = centerChunk.z;
        
        return pos.x >= centerX - squadius &&
               pos.x <= centerX + squadius &&
               pos.z >= centerZ - squadius &&
               pos.z <= centerZ + squadius;
    }
    
    public boolean overlaps(VillageInfo other) {
        return overlaps(other, 0);
    }
    
    public boolean overlaps(VillageInfo other, int minDistance) {
        if (other == null) {
            return false;
        }
        
        ChunkPos thisChunk = townHallPos != null ? new ChunkPos(townHallPos) : villageStartingChunk;
        ChunkPos otherChunk = other.townHallPos != null ? new ChunkPos(other.townHallPos) : other.villageStartingChunk;
        
        int thisMinX = thisChunk.x - squadius - minDistance;
        int thisMaxX = thisChunk.x + squadius + minDistance;
        int thisMinZ = thisChunk.z - squadius - minDistance;
        int thisMaxZ = thisChunk.z + squadius + minDistance;
        
        int otherMinX = otherChunk.x - other.squadius;
        int otherMaxX = otherChunk.x + other.squadius;
        int otherMinZ = otherChunk.z - other.squadius;
        int otherMaxZ = otherChunk.z + other.squadius;
        
        return thisMinX <= otherMaxX && thisMaxX >= otherMinX &&
               thisMinZ <= otherMaxZ && thisMaxZ >= otherMinZ;
    }
    
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putString("VillageId", villageId.toString());
        if (townHallPos != null) {
            tag.putLong("TownHallPos", townHallPos.asLong());
        }
        tag.putLong("VillageStartingChunk", villageStartingChunk.toLong());
        tag.putString("VillageName", villageName);
        tag.putInt("Squadius", squadius);
        return tag;
    }
    
    public static VillageInfo deserializeNBT(CompoundTag tag) {
        if (!tag.contains("VillageId") ||
            !tag.contains("VillageStartingChunk") || !tag.contains("VillageName") || 
            !tag.contains("Squadius")) {
            return null;
        }
        
        try {
            UUID villageId = UUID.fromString(tag.getString("VillageId"));
            BlockPos townHallPos = tag.contains("TownHallPos") ? BlockPos.of(tag.getLong("TownHallPos")) : null;
            ChunkPos villageStartingChunk = new ChunkPos(tag.getLong("VillageStartingChunk"));
            String villageName = tag.getString("VillageName");
            int squadius = tag.getInt("Squadius");
            
            return new VillageInfo(villageId, townHallPos, villageStartingChunk, villageName, squadius);
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
        
        VillageInfo other = (VillageInfo) obj;
        return villageId.equals(other.villageId);
    }
    
    @Override
    public int hashCode() {
        return villageId.hashCode();
    }
}