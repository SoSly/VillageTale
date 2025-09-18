package org.sosly.villagetale.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

public record FoundItem(BlockPos containerPos, ResourceLocation itemId) {
    public static final Codec<FoundItem> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            BlockPos.CODEC.fieldOf("containerPos").forGetter(FoundItem::containerPos),
            ResourceLocation.CODEC.fieldOf("itemId").forGetter(FoundItem::itemId)
        ).apply(instance, FoundItem::new)
    );
}