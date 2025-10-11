package org.sosly.villagetale.data.loaders;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.block.Block;
import org.slf4j.Logger;
import org.sosly.villagetale.data.ItemOrTagMatcher;
import org.sosly.villagetale.data.RecipeTypeInfo;

public class RecipeBlocksDataLoader extends SimpleJsonResourceReloadListener {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<String, RecipeTypeInfo> recipeTypeInfo = new HashMap<>();

    public RecipeBlocksDataLoader() {
        super(GSON, "recipe_blocks");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> resourceMap, ResourceManager resourceManager, ProfilerFiller profiler) {
        recipeTypeInfo.clear();
        LOGGER.info("Loading {} recipe block mapping files", resourceMap.size());

        for (Map.Entry<ResourceLocation, JsonElement> entry : resourceMap.entrySet()) {
            if (!(entry.getValue() instanceof JsonObject jsonObject)) {
                LOGGER.error("Recipe blocks data {} is not a JSON object", entry.getKey());
                continue;
            }

            if (!jsonObject.has("recipe_type") || !jsonObject.has("blocks")) {
                LOGGER.error("Recipe blocks data {} missing required fields", entry.getKey());
                continue;
            }

            String recipeType = jsonObject.get("recipe_type").getAsString();
            JsonArray blocksArray = jsonObject.getAsJsonArray("blocks");
            List<Block> blocks = new ArrayList<>();

            for (JsonElement blockElement : blocksArray) {
                String blockId = blockElement.getAsString();
                ResourceLocation blockLocation = new ResourceLocation(blockId);
                Block block = BuiltInRegistries.BLOCK.get(blockLocation);

                if (block == null) {
                    LOGGER.warn("Unknown block in recipe blocks data: {}", blockId);
                    continue;
                }

                blocks.add(block);
            }

            ItemOrTagMatcher fuelMatcher = null;
            if (jsonObject.has("fuel")) {
                JsonElement fuelElement = jsonObject.get("fuel");
                JsonArray fuelArray;

                if (fuelElement.isJsonArray()) {
                    fuelArray = fuelElement.getAsJsonArray();
                } else {
                    fuelArray = new JsonArray();
                    fuelArray.add(fuelElement.getAsString());
                }

                fuelMatcher = new ItemOrTagMatcher();
                fuelMatcher.loadFromJson(fuelArray);

                if (fuelMatcher.isEmpty()) {
                    fuelMatcher = null;
                }
            }

            if (!blocks.isEmpty()) {
                RecipeTypeInfo info = new RecipeTypeInfo(blocks, fuelMatcher);
                recipeTypeInfo.merge(recipeType, info, (existing, newInfo) -> {
                    List<Block> combinedBlocks = new ArrayList<>(existing.getBlocks());
                    combinedBlocks.addAll(newInfo.getBlocks());
                    ItemOrTagMatcher combinedFuel = newInfo.getFuel().orElse(existing.getFuel().orElse(null));
                    return new RecipeTypeInfo(combinedBlocks, combinedFuel);
                });
                LOGGER.debug("Registered {} blocks for recipe type {}", blocks.size(), recipeType);
            }
        }

        LOGGER.info("Loaded recipe block mappings for {} recipe types", recipeTypeInfo.size());
    }

    public static Map<String, RecipeTypeInfo> getRecipeTypeInfo() {
        return recipeTypeInfo;
    }
}
