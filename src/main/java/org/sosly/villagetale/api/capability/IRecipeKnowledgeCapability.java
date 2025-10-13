package org.sosly.villagetale.api.capability;

import com.google.common.collect.ImmutableSet;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;

public interface IRecipeKnowledgeCapability {
    ImmutableSet<ResourceLocation> known();
    boolean knows(ServerLevel level, ResourceLocation recipeId);
    boolean learn(ServerLevel level, ResourceLocation recipeId);
    boolean forget(ResourceLocation recipeId);
}
