package org.sosly.villagetale.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class BlockOrTagMatcher {
    private final List<Block> blocks = new ArrayList<>();
    private final List<TagKey<Block>> tags = new ArrayList<>();

    public void clear() {
        blocks.clear();
        tags.clear();
    }

    public void loadFromJson(JsonArray array) {
        clear();
        for (JsonElement element : array) {
            String value = element.getAsString();
            if (value.startsWith("#")) {
                ResourceLocation tagId = new ResourceLocation(value.substring(1));
                tags.add(TagKey.create(BuiltInRegistries.BLOCK.key(), tagId));
            } else {
                ResourceLocation blockId = new ResourceLocation(value);
                Block block = BuiltInRegistries.BLOCK.get(blockId);
                if (block != Blocks.AIR) {
                    blocks.add(block);
                }
            }
        }
    }

    public boolean matches(Block block) {
        if (blocks.contains(block)) {
            return true;
        }
        for (TagKey<Block> tag : tags) {
            if (block.builtInRegistryHolder().is(tag)) {
                return true;
            }
        }
        return false;
    }

    public boolean matches(BlockState state) {
        return matches(state.getBlock());
    }

    public boolean matches(Level level, BlockPos pos) {
        return matches(level.getBlockState(pos));
    }

    public boolean isEmpty() {
        return blocks.isEmpty() && tags.isEmpty();
    }

    public List<Block> getBlocks() {
        return blocks;
    }

    public List<TagKey<Block>> getTags() {
        return tags;
    }
}
