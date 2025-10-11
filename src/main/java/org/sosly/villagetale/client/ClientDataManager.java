package org.sosly.villagetale.client;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@OnlyIn(Dist.CLIENT)
public class ClientDataManager {
    private static final Map<Integer, ResourceLocation> professionCache = new HashMap<>();
    private static final Map<Integer, Set<ResourceLocation>> recipesCache = new HashMap<>();

    public static ResourceLocation getCachedProfession(int entityId) {
        return professionCache.get(entityId);
    }

    public static void cacheProfession(int entityId, ResourceLocation professionId) {
        professionCache.put(entityId, professionId);
    }

    public static Set<ResourceLocation> getCachedRecipes(int entityId) {
        return recipesCache.get(entityId);
    }

    public static void cacheRecipes(int entityId, Set<ResourceLocation> recipes) {
        recipesCache.put(entityId, new HashSet<>(recipes));
    }

    public static void clearAll() {
        professionCache.clear();
        recipesCache.clear();
        BoundaryDataStorage.getInstance().clearAll();
        VillageDataManager.getInstance().clear();
        ZoneCreationManager.getInstance().cancel();
    }
}
