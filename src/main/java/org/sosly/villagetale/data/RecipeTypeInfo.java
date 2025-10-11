package org.sosly.villagetale.data;

import java.util.List;
import java.util.Optional;
import net.minecraft.world.level.block.Block;
import org.sosly.villagetale.data.ItemOrTagMatcher;

public class RecipeTypeInfo {
    private final List<Block> blocks;
    private final ItemOrTagMatcher fuel;

    public RecipeTypeInfo(List<Block> blocks, ItemOrTagMatcher fuel) {
        this.blocks = blocks;
        this.fuel = fuel;
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
}
