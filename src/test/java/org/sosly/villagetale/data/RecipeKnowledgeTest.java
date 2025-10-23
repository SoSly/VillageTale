package org.sosly.villagetale.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecipeKnowledgeTest {
    private RecipeKnowledge knowledge;

    @BeforeEach
    void setUp() {
        knowledge = new RecipeKnowledge();
    }

    @Test
    void testKnownReturnsEmptySetInitially() {
        assertTrue(knowledge.known().isEmpty());
    }

    @Test
    void testForgetRemovesRecipe() {
        knowledge.forget(new ResourceLocation("minecraft", "iron_sword"));

        assertTrue(knowledge.known().isEmpty());
    }

    @Test
    void testSerializeEmptyKnowledge() {
        CompoundTag tag = knowledge.serializeNBT();

        assertTrue(tag.contains("recipes"));
        ListTag recipeList = tag.getList("recipes", Tag.TAG_STRING);
        assertEquals(0, recipeList.size());
    }

    @Test
    void testDeserializeEmptyTag() {
        CompoundTag tag = new CompoundTag();

        knowledge.deserializeInto(tag);

        assertTrue(knowledge.known().isEmpty());
    }

    @Test
    void testDeserializeEmptyRecipeList() {
        CompoundTag tag = new CompoundTag();
        tag.put("recipes", new ListTag());

        knowledge.deserializeInto(tag);

        assertTrue(knowledge.known().isEmpty());
    }

    @Test
    void testSerializeAndDeserializeSingleRecipe() {
        CompoundTag tag = new CompoundTag();
        ListTag recipeList = new ListTag();
        recipeList.add(StringTag.valueOf("minecraft:iron_sword"));
        tag.put("recipes", recipeList);

        knowledge.deserializeInto(tag);

        assertEquals(1, knowledge.known().size());
        assertTrue(knowledge.known().contains(new ResourceLocation("minecraft", "iron_sword")));
    }

    @Test
    void testSerializeAndDeserializeMultipleRecipes() {
        CompoundTag tag = new CompoundTag();
        ListTag recipeList = new ListTag();
        recipeList.add(StringTag.valueOf("minecraft:iron_sword"));
        recipeList.add(StringTag.valueOf("minecraft:diamond_pickaxe"));
        recipeList.add(StringTag.valueOf("villagetale:custom_recipe"));
        tag.put("recipes", recipeList);

        knowledge.deserializeInto(tag);

        assertEquals(3, knowledge.known().size());
        assertTrue(knowledge.known().contains(new ResourceLocation("minecraft", "iron_sword")));
        assertTrue(knowledge.known().contains(new ResourceLocation("minecraft", "diamond_pickaxe")));
        assertTrue(knowledge.known().contains(new ResourceLocation("villagetale", "custom_recipe")));
    }

    @Test
    void testSerializeAfterDeserialize() {
        CompoundTag inputTag = new CompoundTag();
        ListTag inputRecipeList = new ListTag();
        inputRecipeList.add(StringTag.valueOf("minecraft:iron_sword"));
        inputRecipeList.add(StringTag.valueOf("minecraft:diamond_pickaxe"));
        inputTag.put("recipes", inputRecipeList);

        knowledge.deserializeInto(inputTag);
        CompoundTag outputTag = knowledge.serializeNBT();

        ListTag outputRecipeList = outputTag.getList("recipes", Tag.TAG_STRING);
        assertEquals(2, outputRecipeList.size());

        boolean hasIronSword = false;
        boolean hasDiamondPickaxe = false;
        for (Tag tag : outputRecipeList) {
            String recipe = tag.getAsString();
            if (recipe.equals("minecraft:iron_sword")) {
                hasIronSword = true;
            }
            if (recipe.equals("minecraft:diamond_pickaxe")) {
                hasDiamondPickaxe = true;
            }
        }

        assertTrue(hasIronSword);
        assertTrue(hasDiamondPickaxe);
    }

    @Test
    void testDeserializeNBTStaticMethod() {
        CompoundTag tag = new CompoundTag();
        ListTag recipeList = new ListTag();
        recipeList.add(StringTag.valueOf("minecraft:iron_sword"));
        recipeList.add(StringTag.valueOf("minecraft:diamond_pickaxe"));
        tag.put("recipes", recipeList);

        RecipeKnowledge deserialized = RecipeKnowledge.deserializeNBT(tag);

        assertEquals(2, deserialized.known().size());
        assertTrue(deserialized.known().contains(new ResourceLocation("minecraft", "iron_sword")));
        assertTrue(deserialized.known().contains(new ResourceLocation("minecraft", "diamond_pickaxe")));
    }

    @Test
    void testForgetRecipeAfterDeserialization() {
        CompoundTag tag = new CompoundTag();
        ListTag recipeList = new ListTag();
        recipeList.add(StringTag.valueOf("minecraft:iron_sword"));
        recipeList.add(StringTag.valueOf("minecraft:diamond_pickaxe"));
        tag.put("recipes", recipeList);

        knowledge.deserializeInto(tag);
        boolean removed = knowledge.forget(new ResourceLocation("minecraft", "iron_sword"));

        assertTrue(removed);
        assertEquals(1, knowledge.known().size());
        assertFalse(knowledge.known().contains(new ResourceLocation("minecraft", "iron_sword")));
        assertTrue(knowledge.known().contains(new ResourceLocation("minecraft", "diamond_pickaxe")));
    }

    @Test
    void testForgetNonExistentRecipe() {
        CompoundTag tag = new CompoundTag();
        ListTag recipeList = new ListTag();
        recipeList.add(StringTag.valueOf("minecraft:iron_sword"));
        tag.put("recipes", recipeList);

        knowledge.deserializeInto(tag);
        boolean removed = knowledge.forget(new ResourceLocation("minecraft", "diamond_pickaxe"));

        assertFalse(removed);
        assertEquals(1, knowledge.known().size());
    }
}
