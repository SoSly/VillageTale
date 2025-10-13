package org.sosly.villagetale.api.capability;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.chunk.LevelChunk;
import org.sosly.villagetale.api.IVillageZone;

/**
 * Capability for storing all data related to a single village.
 * Attached to the chunk containing the village's Town Hall.
 */
public interface IVillageCapability {

    enum Permission {
        NONE {
            @Override
            public String toString() {
                return "NONE";
            }
        },
        OWNER {
            @Override
            public String toString() {
                return "OWNER";
            }
        };

        public abstract String toString();
        public static Permission fromString(String string) {
            return Permission.valueOf(string.toUpperCase());
        }
    }

    /**
     * @return Unique identifier for this village
     */
    UUID getUUID();

    /**
     * Set the unique identifier for this village
     * @param uuid UUID
     */
    void setUUID(UUID uuid);

    /**
     * The chunk the village was created in.
     * @return LevelChunk
     */
    LevelChunk getChunk();

    /**
     * Sets the chunk the village was created in.
     * @param chunk LevelChunk
     */
    void setChunk(LevelChunk chunk);

    /**
     * The name of the village.
     * @return String
     */
    String getName();

    /**
     * Sets the name of the village.
     * @param name String
     */
    void setName(String name);

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
    Set<UUID> getVillagerUUIDs();

    /**
     * Adds a villager to this village.
     * @param villager UUID to add to the village
     */
    void addVillagerByUUID(UUID villager);

    /**
     * Removes a villager from this village.
     * @param villager UUID of villager to remove
     * @return true if villager was found and removed
     */
    boolean removeVillagerByUUID(UUID villager);

    /**
     * @return Map of player UUIDs to their permissions in this village
     */
    Map<UUID, Permission> getPlayerPermissions();

    /**
     * Sets a player's permission level for this village.
     * @param player UUID of player
     * @param permission Permission level to set
     */
    void setPlayerPermission(UUID player, Permission permission);

    /**
     * Checks if a player has the required permission level.
     * @param player UUID of player to check
     * @param required Minimum permission level required
     * @return true if player has required permission or higher
     */
    boolean hasPermission(UUID player, Permission required);
}
