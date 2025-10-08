package org.sosly.villagetale.capability.recipeknowledge;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.sosly.villagetale.api.capability.IRecipeKnowledgeCapability;
import org.sosly.villagetale.capability.Capabilities;

public class RecipeKnowledgeProvider implements ICapabilitySerializable<CompoundTag> {
    private final RecipeKnowledgeCapability capability;
    private final LazyOptional<IRecipeKnowledgeCapability> holder;

    public RecipeKnowledgeProvider() {
        this.capability = new RecipeKnowledgeCapability();
        this.holder = LazyOptional.of(() -> this.capability);
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        if (cap != Capabilities.RECIPE_KNOWLEDGE_CAPABILITY) {
            return LazyOptional.empty();
        }

        return holder.cast();
    }

    @Override
    public CompoundTag serializeNBT() {
        return capability.serializeNBT();
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        capability.deserializeNBT(nbt);
    }
}
