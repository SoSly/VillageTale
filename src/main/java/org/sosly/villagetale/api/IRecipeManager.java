package org.sosly.villagetale.api;

import java.util.Optional;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.block.Block;
import org.sosly.villagetale.data.ItemOrTagMatcher;

public interface IRecipeManager {
    Optional<Block> getCraftingBlock(Recipe<?> recipe);
    boolean requiresFuel(Recipe<?> recipe);
    Optional<ItemOrTagMatcher> getFuelItems(Recipe<?> recipe);
}
