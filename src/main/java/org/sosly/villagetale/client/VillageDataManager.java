package org.sosly.villagetale.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.sosly.villagetale.api.capability.IVillageCapability;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@OnlyIn(Dist.CLIENT)
public class VillageDataManager {
    private static final VillageDataManager INSTANCE = new VillageDataManager();
    private final Map<UUID, IVillageCapability> villages = new HashMap<>();

    private VillageDataManager() {
    }

    public static VillageDataManager getInstance() {
        return INSTANCE;
    }

    public void updateVillageData(UUID villageId, IVillageCapability data) {
        villages.put(villageId, data);
    }

    public IVillageCapability getVillageData(UUID villageId) {
        return villages.get(villageId);
    }

    public void clear() {
        villages.clear();
    }
}
