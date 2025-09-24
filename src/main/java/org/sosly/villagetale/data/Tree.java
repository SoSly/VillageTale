package org.sosly.villagetale.data;

import net.minecraft.core.BlockPos;

import java.util.Set;

public class Tree {
    private final Set<BlockPos> blocks;
    private final BlockPos base;

    public Tree(Set<BlockPos> blocks, BlockPos base) {
        this.blocks = blocks;
        this.base = base;
    }

    public Set<BlockPos> getBlocks() {
        return blocks;
    }

    public BlockPos getBase() {
        return base;
    }
}
