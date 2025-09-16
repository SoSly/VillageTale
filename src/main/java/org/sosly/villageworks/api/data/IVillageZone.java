package org.sosly.villageworks.api.data;

import net.minecraft.core.BlockPos;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

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
    ZoneShape getShape();
    
    /**
     * @return Functional type determining zone behavior and POI scanning
     */
    ZoneType getType();
    
    /**
     * Returns type-specific points of interest within this zone.
     * Results are cached and only rescanned when zone boundaries change.
     * 
     * @return Optional containing POI list, or empty for NONE type zones
     */
    Optional<List<BlockPos>> getPOIs();
}