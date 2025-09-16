package org.sosly.villageworks.api.capability;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import org.sosly.villageworks.api.data.IVillageZone;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Capability for storing all data related to a single village.
 * Attached to the chunk containing the village's Town Hall.
 */
public interface IVillageCapability {
    
    enum Permission {
        NONE,
        OWNER
    }
    
    /**
     * @return Unique identifier for this village
     */
    UUID getVillageId();
    
    /**
     * @return Position of the chunk containing the Town Hall
     */
    ChunkPos getTownHallPos();
    
    /**
     * @return All zones within this village
     */
    List<IVillageZone> getZones();
    
    /**
     * Adds a zone to this village.
     * @param zone Zone to add, must not be null
     */
    void addZone(IVillageZone zone);
    
    /**
     * Removes a zone from this village.
     * @param zoneId UUID of zone to remove
     * @return true if zone was found and removed
     */
    boolean removeZone(UUID zoneId);
    
    /**
     * Finds the zone containing the specified position.
     * @param pos Block position to check
     * @return Zone at position, or null if none found
     */
    IVillageZone getZoneAt(BlockPos pos);
    
    /**
     * @return UUIDs of all villagers assigned to this village
     */
    Set<UUID> getVillagerIds();
    
    /**
     * Assigns a villager to this village.
     * @param villagerId UUID of villager to assign
     */
    void assignVillager(UUID villagerId);
    
    /**
     * Removes a villager from this village.
     * @param villagerId UUID of villager to remove
     * @return true if villager was found and removed
     */
    boolean removeVillager(UUID villagerId);
    
    /**
     * @return Map of player UUIDs to their permissions in this village
     */
    Map<UUID, Permission> getPlayerPermissions();
    
    /**
     * Sets a player's permission level for this village.
     * @param playerId UUID of player
     * @param permission Permission level to set
     */
    void setPlayerPermission(UUID playerId, Permission permission);
    
    /**
     * Checks if a player has the required permission level.
     * @param playerId UUID of player to check
     * @param required Minimum permission level required
     * @return true if player has required permission or higher
     */
    boolean hasPermission(UUID playerId, Permission required);
}