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
import org.sosly.villagetale.data.CraftingMethod;
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

            CraftingMethod craftingMethod = CraftingMethod.FAKE;
            if (jsonObject.has("crafting_method")) {
                String methodString = jsonObject.get("crafting_method").getAsString().toUpperCase();
                try {
                    craftingMethod = CraftingMethod.valueOf(methodString);
                } catch (IllegalArgumentException e) {
                    LOGGER.warn("Unknown crafting method '{}' in {}, defaulting to FAKE", methodString, entry.getKey());
                }
            }

            int[] inputSlots = new int[0];
            if (jsonObject.has("input_slots")) {
                JsonArray inputArray = jsonObject.getAsJsonArray("input_slots");
                inputSlots = new int[inputArray.size()];
                for (int i = 0; i < inputArray.size(); i++) {
                    inputSlots[i] = inputArray.get(i).getAsInt();
                }
            }

            Integer fuelSlot = null;
            if (jsonObject.has("fuel_slot")) {
                fuelSlot = jsonObject.get("fuel_slot").getAsInt();
            }

            int[] outputSlots = new int[0];
            if (jsonObject.has("output_slots")) {
                JsonArray outputArray = jsonObject.getAsJsonArray("output_slots");
                outputSlots = new int[outputArray.size()];
                for (int i = 0; i < outputArray.size(); i++) {
                    outputSlots[i] = outputArray.get(i).getAsInt();
                }
            }

            boolean waitForDrops = false;
            if (jsonObject.has("wait_for_drops")) {
                waitForDrops = jsonObject.get("wait_for_drops").getAsBoolean();
            }

            ResourceLocation craftingSound = null;
            if (jsonObject.has("crafting_sound")) {
                String soundString = jsonObject.get("crafting_sound").getAsString();
                craftingSound = new ResourceLocation(soundString);
            }

            if (!blocks.isEmpty()) {
                RecipeTypeInfo info = new RecipeTypeInfo(blocks, fuelMatcher, craftingMethod,
                        inputSlots, fuelSlot, outputSlots, waitForDrops, craftingSound);
                recipeTypeInfo.merge(recipeType, info, (existing, newInfo) -> {
                    List<Block> combinedBlocks = new ArrayList<>(existing.getBlocks());
                    combinedBlocks.addAll(newInfo.getBlocks());
                    ItemOrTagMatcher combinedFuel = newInfo.getFuel().orElse(existing.getFuel().orElse(null));
                    CraftingMethod combinedMethod = newInfo.getCraftingMethod();
                    int[] combinedInput = newInfo.getInputSlots();
                    Integer combinedFuelSlot = newInfo.getFuelSlot().orElse(existing.getFuelSlot().orElse(null));
                    int[] combinedOutput = newInfo.getOutputSlots();
                    boolean combinedWaitForDrops = newInfo.shouldWaitForDrops() || existing.shouldWaitForDrops();
                    ResourceLocation combinedSound = newInfo.getCraftingSound().orElse(existing.getCraftingSound().orElse(null));
                    return new RecipeTypeInfo(combinedBlocks, combinedFuel, combinedMethod,
                            combinedInput, combinedFuelSlot, combinedOutput, combinedWaitForDrops, combinedSound);
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
