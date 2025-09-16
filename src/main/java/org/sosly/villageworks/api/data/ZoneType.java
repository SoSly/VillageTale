package org.sosly.villageworks.api.data;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.sosly.villageworks.registry.TagKeys;

import java.util.function.Function;
import java.util.function.Predicate;

public enum ZoneType {
    NONE(null),
    STORAGE(pos -> level -> {
        BlockState state = level.getBlockState(pos);
        return state.is(TagKeys.STORAGE_CONTAINERS);
    }),
    TOWNHALL(pos -> level -> {
        BlockState state = level.getBlockState(pos);
        return state.getBlock().getDescriptionId().equals("block.villageworks.townhall");
    }),
    HOME(pos -> level -> {
        BlockState state = level.getBlockState(pos);
        return state.is(BlockTags.BEDS);
    });

    private final POISelector selector;

    ZoneType(POISelector selector) {
        this.selector = selector;
    }

    public boolean isPOI(BlockPos pos, Level level) {
        if (selector == null) {
            return false;
        }
        return selector.apply(pos).test(level);
    }

    @FunctionalInterface
    private interface POISelector extends Function<BlockPos, Predicate<Level>> {
    }
}
