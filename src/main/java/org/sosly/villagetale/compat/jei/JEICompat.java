package org.sosly.villagetale.compat.jei;

import java.util.List;
import java.util.Optional;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.NotNull;
import org.sosly.villagetale.VillageTale;
import org.sosly.villagetale.api.IRecipeManager;
import org.sosly.villagetale.compat.ICompat;
import org.sosly.villagetale.data.CraftingMethod;
import org.sosly.villagetale.data.ItemOrTagMatcher;
import org.sosly.villagetale.data.RecipeManager;

@JeiPlugin
public class JEICompat implements ICompat, IModPlugin, IRecipeManager {
    public static final ResourceLocation ID = new ResourceLocation(VillageTale.MOD_ID, "jei_plugin");
    private static IJeiRuntime runtime;
    private final RecipeManager fallbackRecipeManager = new RecipeManager();

    @Override
    public void setup() {
        VillageTale.LOGGER.info("JEI compatibility layer initialized");
    }

    @Override
    public @NotNull ResourceLocation getPluginUid() {
        return ID;
    }

    @Override
    public void onRuntimeAvailable(@NotNull IJeiRuntime runtime) {
        JEICompat.runtime = runtime;
    }

    @Override
    public Optional<Block> getCraftingBlock(Recipe<?> recipe) {
        if (runtime == null) {
            return Optional.empty();
        }

        ResourceLocation recipeTypeId = new ResourceLocation(recipe.getType().toString());
        Optional<RecipeType<?>> recipeType = runtime.getRecipeManager()
                .getRecipeType(recipeTypeId);

        if (recipeType.isEmpty()) {
            return Optional.empty();
        }

        List<ITypedIngredient<?>> catalysts = runtime.getRecipeManager()
                .createRecipeCatalystLookup(recipeType.get())
                .get()
                .toList();

        if (catalysts.isEmpty()) {
            return Optional.empty();
        }

        ITypedIngredient<?> catalyst = catalysts.get(0);
        Optional<ItemStack> itemStack = catalyst.getItemStack();
        if (itemStack.isEmpty() || itemStack.get().isEmpty()) {
            return Optional.empty();
        }

        Item item = itemStack.get().getItem();
        if (!(item instanceof net.minecraft.world.item.BlockItem blockItem)) {
            return Optional.empty();
        }

        return Optional.of(blockItem.getBlock());
    }

    @Override
    public boolean requiresFuel(Recipe<?> recipe) {
        return fallbackRecipeManager.requiresFuel(recipe);
    }

    @Override
    public Optional<ItemOrTagMatcher> getFuelItems(Recipe<?> recipe) {
        return fallbackRecipeManager.getFuelItems(recipe);
    }

    @Override
    public CraftingMethod getCraftingMethod(Recipe<?> recipe) {
        return fallbackRecipeManager.getCraftingMethod(recipe);
    }

    @Override
    public int[] getInputSlots(Recipe<?> recipe) {
        return fallbackRecipeManager.getInputSlots(recipe);
    }

    @Override
    public Optional<Integer> getFuelSlot(Recipe<?> recipe) {
        return fallbackRecipeManager.getFuelSlot(recipe);
    }

    @Override
    public int[] getOutputSlots(Recipe<?> recipe) {
        return fallbackRecipeManager.getOutputSlots(recipe);
    }

    @Override
    public boolean shouldWaitForDrops(Recipe<?> recipe) {
        return fallbackRecipeManager.shouldWaitForDrops(recipe);
    }

    @Override
    public Optional<ResourceLocation> getCraftingSound(Recipe<?> recipe) {
        return fallbackRecipeManager.getCraftingSound(recipe);
    }
}
