package org.sosly.villagetale.api;

import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.block.Block;
import org.sosly.villagetale.data.CraftingMethod;
import org.sosly.villagetale.data.ItemOrTagMatcher;

public interface IRecipeManager {
    Optional<Block> getCraftingBlock(Recipe<?> recipe);
    boolean requiresFuel(Recipe<?> recipe);
    Optional<ItemOrTagMatcher> getFuelItems(Recipe<?> recipe);
    CraftingMethod getCraftingMethod(Recipe<?> recipe);
    int[] getInputSlots(Recipe<?> recipe);
    Optional<Integer> getFuelSlot(Recipe<?> recipe);
    int[] getOutputSlots(Recipe<?> recipe);
    int[] getOutputSlotsForBlock(Block block);
    boolean shouldWaitForDrops(Recipe<?> recipe);
    Optional<ResourceLocation> getCraftingSound(Recipe<?> recipe);

    boolean doesBlockRequireFuel(Block block);
    Optional<Integer> getFuelSlotForBlock(Block block);
    Optional<ItemOrTagMatcher> getFuelItemsForBlock(Block block);
}
