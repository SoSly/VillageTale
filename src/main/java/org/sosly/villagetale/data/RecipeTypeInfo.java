package org.sosly.villagetale.data;

import java.util.List;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import org.sosly.villagetale.data.matchers.ItemOrTagMatcher;

public class RecipeTypeInfo {
    private final List<Block> blocks;
    private final ItemOrTagMatcher fuel;
    private final CraftingMethod craftingMethod;
    private final int[] inputSlots;
    private final Integer fuelSlot;
    private final int[] outputSlots;
    private final boolean waitForDrops;
    private final ResourceLocation craftingSound;

    public RecipeTypeInfo(List<Block> blocks, ItemOrTagMatcher fuel, CraftingMethod craftingMethod,
                          int[] inputSlots, Integer fuelSlot, int[] outputSlots,
                          boolean waitForDrops, ResourceLocation craftingSound) {
        this.blocks = blocks;
        this.fuel = fuel;
        this.craftingMethod = craftingMethod != null ? craftingMethod : CraftingMethod.FAKE;
        this.inputSlots = inputSlots != null ? inputSlots : new int[0];
        this.fuelSlot = fuelSlot;
        this.outputSlots = outputSlots != null ? outputSlots : new int[0];
        this.waitForDrops = waitForDrops;
        this.craftingSound = craftingSound;
    }

    public List<Block> getBlocks() {
        return blocks;
    }

    public Optional<ItemOrTagMatcher> getFuel() {
        return Optional.ofNullable(fuel);
    }

    public boolean requiresFuel() {
        return fuel != null;
    }

    public CraftingMethod getCraftingMethod() {
        return craftingMethod;
    }

    public int[] getInputSlots() {
        return inputSlots;
    }

    public Optional<Integer> getFuelSlot() {
        return Optional.ofNullable(fuelSlot);
    }

    public int[] getOutputSlots() {
        return outputSlots;
    }

    public boolean shouldWaitForDrops() {
        return waitForDrops;
    }

    public Optional<ResourceLocation> getCraftingSound() {
        return Optional.ofNullable(craftingSound);
    }
}
