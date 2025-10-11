package org.sosly.villagetale.data;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.block.Block;
import org.sosly.villagetale.api.IRecipeManager;
import org.sosly.villagetale.data.loaders.RecipeBlocksDataLoader;

public class RecipeManager implements IRecipeManager {
    @Override
    public Optional<Block> getCraftingBlock(Recipe<?> recipe) {
        String recipeTypeId = recipe.getType().toString();
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
        String recipeTypeId = recipe.getType().toString();
        Map<String, RecipeTypeInfo> recipeTypeInfo = RecipeBlocksDataLoader.getRecipeTypeInfo();

        if (!recipeTypeInfo.containsKey(recipeTypeId)) {
            return false;
        }

        return recipeTypeInfo.get(recipeTypeId).requiresFuel();
    }

    @Override
    public Optional<ItemOrTagMatcher> getFuelItems(Recipe<?> recipe) {
        String recipeTypeId = recipe.getType().toString();
        Map<String, RecipeTypeInfo> recipeTypeInfo = RecipeBlocksDataLoader.getRecipeTypeInfo();

        if (!recipeTypeInfo.containsKey(recipeTypeId)) {
            return Optional.empty();
        }

        return recipeTypeInfo.get(recipeTypeId).getFuel();
    }
}
