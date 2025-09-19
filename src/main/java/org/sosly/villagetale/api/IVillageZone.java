package org.sosly.villagetale.api;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Block;
import org.sosly.villagetale.api.capability.IVillageCapability;

/**
 * Represents a defined area within a village with specific functional purpose.
 * Zones combine physical boundaries (shape) with functional classification (type)
 * to enable villagers to understand and interact with their environment.
 */
public interface IVillageZone {
    /**
     * @return Unique identifier for this zone, never null
     */
    UUID getUUID();

    /**
     * @return Human-readable name for this zone
     */
    String getName();

    /**
     * Updates the zone's display name.
     * @param name New name for the zone, must not be null
     */
    void setName(String name);

    /**
     * @return Physical shape type defining the zone's boundaries
     */
    IZoneShape getShape();

    /**
     * @return Functional type determining zone behavior and POI scanning
     */
    IZoneType getType();

    /**
     * Checks if the specified position is within this zone's boundaries.
     * @param pos Block position to check
     * @return true if position is within zone boundaries
     */
    boolean containsPosition(BlockPos pos);

    /**
     * Gets the starting/reference position for this zone.
     * @return Starting position - center for Radius, first position for Path,
     *         the single position for BlockPos, calculated center for AABB
     */
    BlockPos getStartPosition();

    /**
     * Serializes this zone's data to NBT for persistence.
     * @return CompoundTag containing all zone data
     */
    CompoundTag serializeNBT();

    /**
     * Deserializes zone data from NBT.
     * @param cap IVillageCapability the village containing the zone
     * @param tag CompoundTag containing zone data
     */
    void deserializeNBT(IVillageCapability cap, CompoundTag tag);

    /**
     * @return List of villager UUIDs assigned to work in this zone
     */
    List<UUID> getAssignedVillagers();

    /**
     * Assigns a villager to work in this zone.
     * @param villagerUUID UUID of the villager to assign
     */
    void addAssignedVillager(UUID villagerUUID);

    /**
     * Removes a villager assignment from this zone.
     * @param villagerUUID UUID of the villager to unassign
     * @return true if the villager was assigned and removed, false otherwise
     */
    boolean removeAssignedVillager(UUID villagerUUID);

    /**
     * Returns all possible claims in this zone. Automatically cleans expired claims.
     * @param currentTime Current game time for expiration checking
     * @return Map of claimed positions to claiming villager UUID, empty Optional means unclaimed but claimable
     */
    Map<BlockPos, Optional<UUID>> getClaims(long currentTime);

    /**
     * Get currently active claims, automatically cleaning expired claims.
     * @param currentTime The current game time for expiry checking
     * @param blockFilter Optional filter to only return claims on specific block types
     * @return Map of positions to villager UUIDs for active claims only
     */
    Map<BlockPos, UUID> getActiveClaims(long currentTime, Optional<Predicate<Block>> blockFilter);

    /**
     * Get available (unclaimed) positions, automatically cleaning expired claims.
     * @param currentTime The current game time for expiry checking
     * @param blockFilter Optional filter to only return positions with specific block types
     * @return List of positions that can be claimed
     */
    List<BlockPos> getAvailableClaims(long currentTime, Optional<Predicate<Block>> blockFilter);

    /**
     * Claims a block position for a villager with specified duration.
     * @param pos Position to claim
     * @param villagerUUID UUID of claiming villager
     * @param durationTicks Duration in ticks before claim expires
     * @param currentTime Current game time for expiration checking
     * @return true if position was successfully claimed, false if already claimed or not claimable
     */
    boolean claim(BlockPos pos, UUID villagerUUID, int durationTicks, long currentTime);

    /**
     * Releases a claimed block position.
     * @param pos Position to release
     * @return true if the position was claimed and released, false otherwise
     */
    boolean release(BlockPos pos);
}
