package org.sosly.villagetale.client;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.sosly.villagetale.data.VillageBoundaryData;
import org.sosly.villagetale.data.ZoneBoundaryData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@OnlyIn(Dist.CLIENT)
public class BoundaryDataStorage {
    private static final BoundaryDataStorage INSTANCE = new BoundaryDataStorage();

    private final Map<ResourceKey<Level>, DimensionBoundaryData> dimensionData = new HashMap<>();
    private boolean overlayEnabled = false;

    private BoundaryDataStorage() {}

    public static BoundaryDataStorage getInstance() {
        return INSTANCE;
    }

    public void addVillage(ResourceKey<Level> dimension, VillageBoundaryData village) {
        if (dimension == null || village == null) {
            return;
        }

        getDimensionData(dimension).villages.put(village.getVillageId(), village);
    }

    public void removeVillage(ResourceKey<Level> dimension, UUID villageId) {
        if (dimension == null || villageId == null) {
            return;
        }

        getDimensionData(dimension).villages.remove(villageId);
    }

    public void addZone(ResourceKey<Level> dimension, ZoneBoundaryData zone) {
        if (dimension == null || zone == null) {
            return;
        }

        getDimensionData(dimension).zones.put(zone.getZoneId(), zone);
    }

    public void removeZone(ResourceKey<Level> dimension, UUID zoneId) {
        if (dimension == null || zoneId == null) {
            return;
        }

        getDimensionData(dimension).zones.remove(zoneId);
    }

    public void updateZone(ResourceKey<Level> dimension, ZoneBoundaryData zone) {
        addZone(dimension, zone);
    }

    public void updateVillage(ResourceKey<Level> dimension, VillageBoundaryData village) {
        addVillage(dimension, village);
    }

    public void clearDimension(ResourceKey<Level> dimension) {
        if (dimension == null) {
            return;
        }

        dimensionData.remove(dimension);
    }

    public void clearAll() {
        dimensionData.clear();
    }

    public DimensionBoundaryData getDimensionData(ResourceKey<Level> dimension) {
        if (dimension == null) {
            return new DimensionBoundaryData();
        }
        return dimensionData.computeIfAbsent(dimension, k -> new DimensionBoundaryData());
    }

    public boolean isOverlayEnabled() {
        return overlayEnabled;
    }

    public void setOverlayEnabled(boolean enabled) {
        this.overlayEnabled = enabled;
    }

    public void toggleOverlay() {
        this.overlayEnabled = !this.overlayEnabled;
    }

    public static class DimensionBoundaryData {
        private final Map<UUID, VillageBoundaryData> villages = new HashMap<>();
        private final Map<UUID, ZoneBoundaryData> zones = new HashMap<>();

        public Map<UUID, VillageBoundaryData> getVillages() {
            return villages;
        }

        public Map<UUID, ZoneBoundaryData> getZones() {
            return zones;
        }
    }
}
