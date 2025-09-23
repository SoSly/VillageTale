package org.sosly.villagetale.api.capability;

import com.google.common.collect.ImmutableSet;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;

public interface IRecipeKnowledgeCapability {
    public ImmutableSet<ResourceLocation> known();
    public boolean knows(ServerLevel level, ResourceLocation recipeId);
    public boolean learn(ServerLevel level, ResourceLocation recipeId);
}
