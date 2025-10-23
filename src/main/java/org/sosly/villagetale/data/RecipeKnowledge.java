package org.sosly.villagetale.data;

import com.google.common.collect.ImmutableSet;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import org.sosly.villagetale.api.IRecipeKnowledge;

public class RecipeKnowledge implements IRecipeKnowledge {
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

    @Override
    public boolean forget(ResourceLocation recipeId) {
        return recipes.remove(recipeId);
    }

    private Optional<? extends Recipe<?>> getRecipe(ServerLevel level, ResourceLocation recipeId) {
        RecipeManager recipeManager = level.getRecipeManager();
        return recipeManager.byKey(recipeId);
    }

    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();

        ListTag recipeList = new ListTag();
        for (ResourceLocation recipe : recipes) {
            recipeList.add(StringTag.valueOf(recipe.toString()));
        }
        tag.put("recipes", recipeList);

        return tag;
    }

    public void deserializeInto(CompoundTag nbt) {
        if (!nbt.contains("recipes", Tag.TAG_LIST)) {
            return;
        }

        ListTag recipeList = nbt.getList("recipes", Tag.TAG_STRING);
        for (Tag tag : recipeList) {
            recipes.add(new ResourceLocation(tag.getAsString()));
        }
    }

    public static RecipeKnowledge deserializeNBT(CompoundTag nbt) {
        RecipeKnowledge knowledge = new RecipeKnowledge();
        knowledge.deserializeInto(nbt);
        return knowledge;
    }
}
