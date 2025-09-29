package org.sosly.villagetale.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class ItemOrTagMatcher {
    private final List<Item> items = new ArrayList<>();
    private final List<TagKey<Item>> tags = new ArrayList<>();
    
    public void clear() {
        items.clear();
        tags.clear();
    }
    
    public void loadFromJson(JsonArray array) {
        clear();
        for (JsonElement element : array) {
            String value = element.getAsString();
            if (value.startsWith("#")) {
                ResourceLocation tagId = new ResourceLocation(value.substring(1));
                tags.add(TagKey.create(BuiltInRegistries.ITEM.key(), tagId));
            } else {
                ResourceLocation itemId = new ResourceLocation(value);
                Item item = BuiltInRegistries.ITEM.get(itemId);
                if (item != null && item != net.minecraft.world.item.Items.AIR) {
                    items.add(item);
                }
            }
        }
    }
    
    public boolean matches(Item item) {
        if (items.contains(item)) {
            return true;
        }
        for (TagKey<Item> tag : tags) {
            if (item.builtInRegistryHolder().is(tag)) {
                return true;
            }
        }
        return false;
    }
    
    public boolean matches(ItemStack stack) {
        return matches(stack.getItem());
    }
    
    public boolean isEmpty() {
        return items.isEmpty() && tags.isEmpty();
    }
    
    public List<Item> getItems() {
        return items;
    }
    
    public List<TagKey<Item>> getTags() {
        return tags;
    }
}