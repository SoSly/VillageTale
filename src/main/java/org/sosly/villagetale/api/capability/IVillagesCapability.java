package org.sosly.villagetale.api.capability;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import org.sosly.villagetale.data.VillageInfo;

import java.util.Collection;
import java.util.UUID;

/**
 * Capability for managing all villages within a dimension.
 * Attached to Level entities and handles village creation, removal, and lookup operations.
 */
public interface IVillagesCapability {

    /**
     * Finds the village that contains the specified chunk position.
     * @param pos Chunk position to check
     * @return Village containing the chunk, or null if no village found
     */
    VillageInfo getVillageAt(ChunkPos pos);

    /**
     * Creates a new village with the specified parameters.
     * @param townHallPos Block position for the town hall
     * @param villageName Unique name for the village
     * @param squadius Square radius in chunks (1-16)
     * @return UUID of created village, or null if creation failed
     */
    UUID createVillage(BlockPos townHallPos, String villageName, int squadius);

    /**
     * Removes a village by its unique identifier.
     * @param villageId UUID of village to remove
     * @return true if village was found and removed
     */
    boolean removeVillage(UUID villageId);

    /**
     * Checks if a chunk can be claimed without overlapping existing villages.
     * @param pos Chunk position to check
     * @param excludeVillageId Village UUID to exclude from overlap check, or null
     * @return true if chunk can be claimed
     */
    boolean canClaimChunk(ChunkPos pos, UUID excludeVillageId);

    /**
     * @return All villages in this dimension
     */
    Collection<VillageInfo> getVillages();

    /**
     * Finds a village by its unique identifier.
     * @param villageId UUID to search for
     * @return Village with matching UUID, or null if not found
     */
    VillageInfo getVillageById(UUID villageId);

    /**
     * Finds a village by its name.
     * @param villageName Name to search for
     * @return Village with matching name, or null if not found
     */
    VillageInfo getVillageByName(String villageName);

    /**
     * Updates the town hall position for a village.
     * @param villageId UUID of village to update
     * @param newPos New position for the town hall, or null to clear
     * @return true if village was found and updated
     */
    boolean updateTownHallPos(UUID villageId, BlockPos newPos);

    /**
     * Updates the name of a village.
     * @param villageId UUID of village to update
     * @param newName New name for the village
     * @return true if village was found and name was updated successfully
     */
    boolean updateVillageName(UUID villageId, String newName);
}
