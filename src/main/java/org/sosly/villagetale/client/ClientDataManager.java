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
    private static final Map<Integer, ResourceLocation> PROFESSION_CACHE = new HashMap<>();
    private static final Map<Integer, Set<ResourceLocation>> RECIPES_CACHE = new HashMap<>();

    public static ResourceLocation getCachedProfession(int entityId) {
        return PROFESSION_CACHE.get(entityId);
    }

    public static void cacheProfession(int entityId, ResourceLocation professionId) {
        PROFESSION_CACHE.put(entityId, professionId);
    }

    public static Set<ResourceLocation> getCachedRecipes(int entityId) {
        return RECIPES_CACHE.get(entityId);
    }

    public static void cacheRecipes(int entityId, Set<ResourceLocation> recipes) {
        RECIPES_CACHE.put(entityId, new HashSet<>(recipes));
    }

    public static void clearAll() {
        PROFESSION_CACHE.clear();
        RECIPES_CACHE.clear();
        BoundaryDataStorage.getInstance().clearAll();
        VillageDataManager.getInstance().clear();
        ZoneCreationManager.getInstance().cancel();
    }
}
