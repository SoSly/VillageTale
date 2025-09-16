package org.sosly.villageworks.registry;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import org.sosly.villageworks.VillageWorks;

public class TagKeys {
    public static final TagKey<Block> STORAGE_CONTAINERS = create("storage_containers");

    private static TagKey<Block> create(String name) {
        return TagKey.create(net.minecraft.core.registries.Registries.BLOCK, new ResourceLocation(VillageWorks.MOD_ID, name));
    }
}
