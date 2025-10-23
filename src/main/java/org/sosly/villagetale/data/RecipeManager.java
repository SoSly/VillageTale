package org.sosly.villagetale.data;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.ForgeRegistries;
import org.sosly.villagetale.api.IRecipeManager;
import org.sosly.villagetale.data.loaders.RecipeBlocksDataLoader;
import org.sosly.villagetale.data.matchers.ItemOrTagMatcher;

public class RecipeManager implements IRecipeManager {
    private static final RecipeManager INSTANCE = new RecipeManager();

    public static RecipeManager getInstance() {
        return INSTANCE;
    }

    private String getRecipeTypeId(Recipe<?> recipe) {
        ResourceLocation recipeTypeLocation = ForgeRegistries.RECIPE_TYPES.getKey(recipe.getType());
        return recipeTypeLocation != null ? recipeTypeLocation.toString() : recipe.getType().toString();
    }

    @Override
    public Optional<Block> getCraftingBlock(Recipe<?> recipe) {
        String recipeTypeId = getRecipeTypeId(recipe);
        Map<String, RecipeTypeInfo> recipeTypeInfo = RecipeBlocksDataLoader.getRecipeTypeInfo();

        if (!recipeTypeInfo.containsKey(recipeTypeId)) {
            return Optional.empty();
        }

        RecipeTypeInfo info = recipeTypeInfo.get(recipeTypeId);
        List<Block> blocks = info.getBlocks();
        if (blocks.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(blocks.get(0));
    }

    @Override
    public boolean requiresFuel(Recipe<?> recipe) {
        String recipeTypeId = getRecipeTypeId(recipe);
        Map<String, RecipeTypeInfo> recipeTypeInfo = RecipeBlocksDataLoader.getRecipeTypeInfo();

        if (!recipeTypeInfo.containsKey(recipeTypeId)) {
            return false;
        }

        return recipeTypeInfo.get(recipeTypeId).requiresFuel();
    }

    @Override
    public Optional<ItemOrTagMatcher> getFuelItems(Recipe<?> recipe) {
        String recipeTypeId = getRecipeTypeId(recipe);
        Map<String, RecipeTypeInfo> recipeTypeInfo = RecipeBlocksDataLoader.getRecipeTypeInfo();

        if (!recipeTypeInfo.containsKey(recipeTypeId)) {
            return Optional.empty();
        }

        return recipeTypeInfo.get(recipeTypeId).getFuel();
    }

    @Override
    public CraftingMethod getCraftingMethod(Recipe<?> recipe) {
        String recipeTypeId = getRecipeTypeId(recipe);
        Map<String, RecipeTypeInfo> recipeTypeInfo = RecipeBlocksDataLoader.getRecipeTypeInfo();

        if (!recipeTypeInfo.containsKey(recipeTypeId)) {
            return CraftingMethod.FAKE;
        }

        return recipeTypeInfo.get(recipeTypeId).getCraftingMethod();
    }

    @Override
    public int[] getInputSlots(Recipe<?> recipe) {
        String recipeTypeId = getRecipeTypeId(recipe);
        Map<String, RecipeTypeInfo> recipeTypeInfo = RecipeBlocksDataLoader.getRecipeTypeInfo();

        if (!recipeTypeInfo.containsKey(recipeTypeId)) {
            return new int[0];
        }

        return recipeTypeInfo.get(recipeTypeId).getInputSlots();
    }

    @Override
    public Optional<Integer> getFuelSlot(Recipe<?> recipe) {
        String recipeTypeId = getRecipeTypeId(recipe);
        Map<String, RecipeTypeInfo> recipeTypeInfo = RecipeBlocksDataLoader.getRecipeTypeInfo();

        if (!recipeTypeInfo.containsKey(recipeTypeId)) {
            return Optional.empty();
        }

        return recipeTypeInfo.get(recipeTypeId).getFuelSlot();
    }

    @Override
    public int[] getOutputSlots(Recipe<?> recipe) {
        String recipeTypeId = getRecipeTypeId(recipe);
        Map<String, RecipeTypeInfo> recipeTypeInfo = RecipeBlocksDataLoader.getRecipeTypeInfo();

        if (!recipeTypeInfo.containsKey(recipeTypeId)) {
            return new int[0];
        }

        return recipeTypeInfo.get(recipeTypeId).getOutputSlots();
    }

    @Override
    public boolean shouldWaitForDrops(Recipe<?> recipe) {
        String recipeTypeId = getRecipeTypeId(recipe);
        Map<String, RecipeTypeInfo> recipeTypeInfo = RecipeBlocksDataLoader.getRecipeTypeInfo();

        if (!recipeTypeInfo.containsKey(recipeTypeId)) {
            return false;
        }

        return recipeTypeInfo.get(recipeTypeId).shouldWaitForDrops();
    }

    @Override
    public Optional<ResourceLocation> getCraftingSound(Recipe<?> recipe) {
        String recipeTypeId = getRecipeTypeId(recipe);
        Map<String, RecipeTypeInfo> recipeTypeInfo = RecipeBlocksDataLoader.getRecipeTypeInfo();

        if (!recipeTypeInfo.containsKey(recipeTypeId)) {
            return Optional.empty();
        }

        return recipeTypeInfo.get(recipeTypeId).getCraftingSound();
    }

    @Override
    public int[] getOutputSlotsForBlock(Block block) {
        Map<String, RecipeTypeInfo> recipeTypeInfo = RecipeBlocksDataLoader.getRecipeTypeInfo();

        for (RecipeTypeInfo info : recipeTypeInfo.values()) {
            if (info.getBlocks().contains(block)) {
                return info.getOutputSlots();
            }
        }

        return new int[0];
    }

    @Override
    public boolean doesBlockRequireFuel(Block block) {
        Map<String, RecipeTypeInfo> recipeTypeInfo = RecipeBlocksDataLoader.getRecipeTypeInfo();

        for (RecipeTypeInfo info : recipeTypeInfo.values()) {
            if (info.getBlocks().contains(block)) {
                return info.requiresFuel();
            }
        }

        return false;
    }

    @Override
    public Optional<Integer> getFuelSlotForBlock(Block block) {
        Map<String, RecipeTypeInfo> recipeTypeInfo = RecipeBlocksDataLoader.getRecipeTypeInfo();

        for (RecipeTypeInfo info : recipeTypeInfo.values()) {
            if (info.getBlocks().contains(block)) {
                return info.getFuelSlot();
            }
        }

        return Optional.empty();
    }

    @Override
    public Optional<ItemOrTagMatcher> getFuelItemsForBlock(Block block) {
        Map<String, RecipeTypeInfo> recipeTypeInfo = RecipeBlocksDataLoader.getRecipeTypeInfo();

        for (RecipeTypeInfo info : recipeTypeInfo.values()) {
            if (info.getBlocks().contains(block)) {
                return info.getFuel();
            }
        }

        return Optional.empty();
    }
}
