package org.sosly.villagetale.client;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.HashMap;
import java.util.Map;

@OnlyIn(Dist.CLIENT)
public class ClientDataManager {
    private static final Map<Integer, ResourceLocation> professionCache = new HashMap<>();

    public static ResourceLocation getCachedProfession(int entityId) {
        return professionCache.get(entityId);
    }

    public static void cacheProfession(int entityId, ResourceLocation professionId) {
        professionCache.put(entityId, professionId);
    }

    public static void clearAll() {
        professionCache.clear();
        BoundaryDataStorage.getInstance().clearAll();
        VillageDataManager.getInstance().clear();
        ZoneCreationManager.getInstance().cancel();
    }
}
