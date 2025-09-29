package org.sosly.villagetale.data.loaders;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;
import org.sosly.villagetale.api.IZoneType;
import org.sosly.villagetale.zone.ZoneRegistry;

public class ZoneTypeDataLoader extends SimpleJsonResourceReloadListener {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public ZoneTypeDataLoader() {
        super(GSON, "zone_types");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> resourceMap, ResourceManager resourceManager, ProfilerFiller profiler) {
        LOGGER.info("Loading {} zone type data files", resourceMap.size());

        for (Map.Entry<ResourceLocation, JsonElement> entry : resourceMap.entrySet()) {
            if (!(entry.getValue() instanceof JsonObject jsonObject)) {
                LOGGER.error("Zone type data {} is not a JSON object", entry.getKey());
                return;
            }

            ResourceLocation zoneTypeId = new ResourceLocation(entry.getKey().getNamespace(), entry.getKey().getPath());
            IZoneType type = ZoneRegistry.INSTANCE.type(zoneTypeId);
            if (type == null) {
                LOGGER.warn("No registered zone type found for data: {}", zoneTypeId);
                return;
            }

            type.onDatapackReload(jsonObject);
        }
    }
}
