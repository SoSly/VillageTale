package org.sosly.villagetale.api;

import java.util.Optional;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.block.Block;

public interface IRecipeManager {
    Optional<Block> getCraftingBlock(Recipe<?> recipe);
}
