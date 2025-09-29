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
import org.sosly.villagetale.api.IProfession;
import org.sosly.villagetale.profession.ProfessionRegistry;

public class ProfessionDataLoader extends SimpleJsonResourceReloadListener {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public ProfessionDataLoader() {
        super(GSON, "professions");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> resourceMap, ResourceManager resourceManager, ProfilerFiller profiler) {
        LOGGER.info("Loading {} profession data files", resourceMap.size());

        for (Map.Entry<ResourceLocation, JsonElement> entry : resourceMap.entrySet()) {
            if (!(entry.getValue() instanceof JsonObject jsonObject)) {
                LOGGER.error("Profession data {} is not a JSON object", entry.getKey());
                return;
            }

            ResourceLocation professionId = new ResourceLocation(entry.getKey().getNamespace(), entry.getKey().getPath());
            IProfession profession = ProfessionRegistry.INSTANCE.getProfession(professionId).orElse(null);
            if (profession == null) {
                LOGGER.warn("No registered profession found for data: {}", professionId);
                return;
            }
            profession.onDatapackReload(jsonObject);
            LOGGER.debug("Loaded data for profession: {}", professionId);
        }
    }
}
