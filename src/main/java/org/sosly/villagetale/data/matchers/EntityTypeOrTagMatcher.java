package org.sosly.villagetale.data.matchers;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

public class EntityTypeOrTagMatcher {
    private final List<EntityType<?>> entityTypes = new ArrayList<>();
    private final List<TagKey<EntityType<?>>> tags = new ArrayList<>();

    public void clear() {
        entityTypes.clear();
        tags.clear();
    }

    public void loadFromJson(JsonArray array) {
        clear();
        for (JsonElement element : array) {
            String value = element.getAsString();
            if (value.startsWith("#")) {
                ResourceLocation tagId = new ResourceLocation(value.substring(1));
                tags.add(TagKey.create(BuiltInRegistries.ENTITY_TYPE.key(), tagId));
            } else {
                ResourceLocation entityTypeId = new ResourceLocation(value);
                EntityType<?> entityType = BuiltInRegistries.ENTITY_TYPE.get(entityTypeId);
                if (entityType != null) {
                    entityTypes.add(entityType);
                }
            }
        }
    }

    public boolean matches(EntityType<?> entityType) {
        if (entityTypes.contains(entityType)) {
            return true;
        }
        for (TagKey<EntityType<?>> tag : tags) {
            if (entityType.builtInRegistryHolder().is(tag)) {
                return true;
            }
        }
        return false;
    }

    public boolean matches(Entity entity) {
        return matches(entity.getType());
    }

    public boolean isEmpty() {
        return entityTypes.isEmpty() && tags.isEmpty();
    }

    public List<EntityType<?>> getEntityTypes() {
        return entityTypes;
    }

    public List<TagKey<EntityType<?>>> getTags() {
        return tags;
    }

    public List<ResourceLocation> getAllEntityTypeIds() {
        List<ResourceLocation> ids = new ArrayList<>();
        for (EntityType<?> type : entityTypes) {
            ids.add(BuiltInRegistries.ENTITY_TYPE.getKey(type));
        }
        for (TagKey<EntityType<?>> tag : tags) {
            BuiltInRegistries.ENTITY_TYPE.getTagOrEmpty(tag)
                .forEach(holder -> ids.add(BuiltInRegistries.ENTITY_TYPE.getKey(holder.value())));
        }
        return ids;
    }
}
