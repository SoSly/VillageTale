package org.sosly.villagetale.data;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import org.sosly.villagetale.VillageTale;

public class TagKeys {
    public static final TagKey<Block> PLANTABLE = create("plantable");
    public static final TagKey<Block> STORAGE_CONTAINERS = create("storage_containers");
    public static final TagKey<Block> TILLABLE = create("tillable");

    private static TagKey<Block> create(String name) {
        return TagKey.create(net.minecraft.core.registries.Registries.BLOCK, new ResourceLocation(VillageTale.MOD_ID, name));
    }
}
