package org.sosly.villagetale.data.loaders;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;
import org.sosly.villagetale.data.ItemOrTagMatcher;

public class EntityDataLoader extends SimpleJsonResourceReloadListener {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final Map<ResourceLocation, EntityData> ENTITY_DATA = new HashMap<>();

    public EntityDataLoader() {
        super(GSON, "entities");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> resourceMap, ResourceManager resourceManager, ProfilerFiller profiler) {
        LOGGER.info("Loading {} entity data files", resourceMap.size());

        ENTITY_DATA.clear();

        for (Map.Entry<ResourceLocation, JsonElement> entry : resourceMap.entrySet()) {
            if (!(entry.getValue() instanceof JsonObject jsonObject)) {
                LOGGER.error("Entity data {} is not a JSON object", entry.getKey());
                return;
            }

            EntityData data = new EntityData();

            if (jsonObject.has("food")) {
                data.food.loadFromJson(jsonObject.getAsJsonArray("food"));
            }

            if (jsonObject.has("tools")) {
                data.tools.loadFromJson(jsonObject.getAsJsonArray("tools"));
            }

            String path = entry.getKey().getPath();
            ResourceLocation entityId;
            if (path.contains("/")) {
                String[] parts = path.split("/", 2);
                entityId = new ResourceLocation(parts[0], parts[1]);
            } else {
                entityId = new ResourceLocation(entry.getKey().getNamespace(), path);
            }
            ENTITY_DATA.put(entityId, data);
        }
    }

    public static EntityData getEntityData(ResourceLocation entityId) {
        return ENTITY_DATA.getOrDefault(entityId, new EntityData());
    }

    public static class EntityData {
        public final ItemOrTagMatcher food = new ItemOrTagMatcher();
        public final ItemOrTagMatcher tools = new ItemOrTagMatcher();
    }
}
