package org.sosly.villagetale.api;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
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
     * Checks if the specified position is within this zone's boundaries with a buffer.
     * @param pos Block position to check
     * @param buffer Number of blocks to expand the zone boundary (in X and Z directions)
     * @return true if position is within the expanded zone boundaries
     */
    default boolean containsPosition(BlockPos pos, int buffer) {
        for (int x = -buffer; x <= buffer; x++) {
            for (int z = -buffer; z <= buffer; z++) {
                if (containsPosition(pos.offset(x, 0, z))) {
                    return true;
                }
            }
        }
        return false;
    }

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
    Map<BlockPos, UUID> getActiveClaims(long currentTime, Optional<Predicate<BlockState>> blockFilter);

    /**
     * Get available (unclaimed) positions, automatically cleaning expired claims.
     * @param currentTime The current game time for expiry checking
     * @param blockFilter Optional filter to only return positions with specific block types
     * @return List of positions that can be claimed
     */
    List<BlockPos> getAvailableClaims(long currentTime, Optional<Predicate<BlockState>> blockFilter);

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

    /**
     * Gets the item filter for this zone.
     * Empty list means all items are acceptable for zone operations.
     * Non-empty list restricts operations to only these specific items.
     * Usage varies by zone type (e.g., crops to plant for farmland, items to store for storage).
     * @return List of allowed items as ItemStacks
     */
    List<ItemStack> getFilter();

    /**
     * Sets the item filter for this zone.
     * @param filter List of allowed items as ItemStacks
     */
    void setFilter(List<ItemStack> filter);

    /**
     * Claims an entity for a villager with specified duration.
     * @param entityId UUID of entity to claim
     * @param villagerUUID UUID of claiming villager
     * @param durationTicks Duration in ticks before claim expires
     * @param currentTime Current game time for expiration checking
     * @return true if entity was successfully claimed, false if already claimed
     */
    boolean claim(UUID entityId, UUID villagerUUID, int durationTicks, long currentTime);

    /**
     * Releases a claimed entity.
     * @param entityId UUID of entity to release
     * @return true if the entity was claimed and released, false otherwise
     */
    boolean release(UUID entityId);

    /**
     * Returns all entity claims in this zone. Automatically cleans expired claims.
     * @param currentTime Current game time for expiration checking
     * @return Map of entity UUIDs to claiming villager UUID, empty Optional means unclaimed
     */
    Map<UUID, Optional<UUID>> getEntityClaims(long currentTime);

    /**
     * Get currently active entity claims, automatically cleaning expired claims.
     * @param currentTime The current game time for expiry checking
     * @return Map of entity UUIDs to villager UUIDs for active claims only
     */
    Map<UUID, UUID> getActiveEntityClaims(long currentTime);

    /**
     * Get available (unclaimed) entities in the zone.
     * @param currentTime The current game time for expiry checking
     * @return List of entity UUIDs that can be claimed
     */
    List<UUID> getAvailableEntityClaims(long currentTime);
}
