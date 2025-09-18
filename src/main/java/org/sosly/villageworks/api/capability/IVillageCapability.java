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
    
    /**
     * Assigns a villager to work in the specified zone.
     * @param zoneId UUID of the zone
     * @param villagerUUID UUID of the villager to assign
     * @return true if zone was found and villager assigned
     */
    boolean assignVillagerToZone(UUID zoneId, UUID villagerUUID);
    
    /**
     * Removes a villager assignment from the specified zone.
     * @param zoneId UUID of the zone
     * @param villagerUUID UUID of the villager to unassign
     * @return true if zone was found and villager was assigned
     */
    boolean unassignVillagerFromZone(UUID zoneId, UUID villagerUUID);
    
    /**
     * Claims a block position for a villager in the specified zone.
     * @param zoneId UUID of the zone
     * @param pos Position to claim
     * @param villagerUUID UUID of claiming villager
     * @param durationTicks Duration in ticks before claim expires
     * @param currentTime Current game time
     * @return true if zone was found and position was claimable
     */
    boolean claimPositionInZone(UUID zoneId, BlockPos pos, UUID villagerUUID, int durationTicks, long currentTime);
    
    /**
     * Releases a claimed block position in the specified zone.
     * @param zoneId UUID of the zone
     * @param pos Position to release
     * @return true if zone was found and position was claimed
     */
    boolean releasePositionInZone(UUID zoneId, BlockPos pos);
}