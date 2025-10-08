package org.sosly.villagetale.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.sosly.villagetale.client.data.ClientVillageData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@OnlyIn(Dist.CLIENT)
public class VillageDataManager {
    private static final VillageDataManager INSTANCE = new VillageDataManager();
    private final Map<UUID, ClientVillageData> villages = new HashMap<>();

    private VillageDataManager() {
    }

    public static VillageDataManager getInstance() {
        return INSTANCE;
    }

    public void updateVillageData(UUID villageId, ClientVillageData data) {
        villages.put(villageId, data);
    }

    public ClientVillageData getVillageData(UUID villageId) {
        return villages.get(villageId);
    }

    public void clear() {
        villages.clear();
    }
}
