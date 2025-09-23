package org.sosly.villagetale.capability.recipeknowledge;

import com.google.common.collect.ImmutableSet;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import org.sosly.villagetale.api.capability.IRecipeKnowledgeCapability;

public class RecipeKnowledgeCapability implements IRecipeKnowledgeCapability {
    private final Set<ResourceLocation> recipes = new HashSet<>();

    @Override
    public ImmutableSet<ResourceLocation> known() {
        return ImmutableSet.copyOf(recipes);
    }

    @Override
    public boolean knows(ServerLevel level, ResourceLocation recipeId) {
        Optional<? extends Recipe<?>> recipe = getRecipe(level, recipeId);
        if (recipe.isEmpty()) {
            recipes.remove(recipeId);
            return false;
        }

        return recipes.contains(recipeId);
    }

    @Override
    public boolean learn(ServerLevel level, ResourceLocation recipeId) {
        Optional<? extends Recipe<?>> recipe = getRecipe(level, recipeId);
        if (recipe.isEmpty()) {
            return false;
        }

        if (recipes.contains(recipeId)) {
            return false;
        }

        return recipes.add(recipeId);
    }

    public void addRecipe(ResourceLocation recipeId) {
        recipes.add(recipeId);
    }

    public Set<ResourceLocation> getRecipes() {
        return recipes;
    }

    private Optional<? extends Recipe<?>> getRecipe(ServerLevel level, ResourceLocation recipeId) {
        RecipeManager recipeManager = level.getRecipeManager();
        return recipeManager.byKey(recipeId);
    }
}
