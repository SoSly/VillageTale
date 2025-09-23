package org.sosly.villagetale.helper;

import com.google.common.collect.ImmutableSet;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.crafting.Recipe;
import org.sosly.villagetale.api.capability.IRecipeKnowledgeCapability;
import org.sosly.villagetale.capability.Capabilities;
import org.sosly.villagetale.entity.MemoryModuleTypes;
import org.sosly.villagetale.entity.Villager;

public class VillagerHelper {
    public static Optional<ResourceLocation> getRandomRecipe(ServerLevel level, Villager villager) {
        IRecipeKnowledgeCapability knowledge = villager.getCapability(Capabilities.RECIPE_KNOWLEDGE_CAPABILITY)
                .orElseThrow(() -> new IllegalStateException("No knowledge capability found"));

        ImmutableSet<ResourceLocation> recipes = knowledge.known();
        if (recipes.isEmpty()) {
            return Optional.empty();
        }
        int index = level.getRandom().nextInt(recipes.size());
        ResourceLocation recipe = recipes.asList().get(index);
        return recipes.contains(recipe) ? Optional.of(recipe) : Optional.empty();
    }

    public static Recipe<?> getCurrentRecipe(ServerLevel level, Villager villager) {
        ResourceLocation recipeId = villager.getBrain()
                .getMemory(MemoryModuleTypes.CURRENT_RECIPE.get())
                .orElse(null);

        if (recipeId == null) {
            return null;
        }

        Recipe<?> recipe = level.getRecipeManager().byKey(recipeId).orElse(null);
        if (recipe == null) {
            return null;
        }

        return recipe;
    }
}
