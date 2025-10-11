package org.sosly.villagetale.helper;

import com.mojang.datafixers.util.Pair;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import org.sosly.villagetale.api.IVillageZone;
import org.sosly.villagetale.api.IWantedItem;
import org.sosly.villagetale.data.WantedItem;
import org.sosly.villagetale.entity.MemoryModuleTypes;
import org.sosly.villagetale.entity.Villager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public enum ItemMatcher {
    PROFESSION_TOOL {
        @Override
        public List<IWantedItem> getFor(Villager villager) {
            IWantedItem tool = villager.getProfession().getTool().orElse(null);
            if (tool != null) {
                return List.of(tool);
            }
            return List.of();
        }
    },
    FOOD {
        @Override
        public List<IWantedItem> getFor(Villager villager) {
            return List.of(new WantedItem(ItemStack::isEdible, 3, 0));
        }
    },
    RESOURCES {
        @Override
        public List<IWantedItem> getFor(Villager villager) {
            if (!villager.getBrain().hasMemoryValue(MemoryModuleTypes.VILLAGE.get())) {
                return List.of();
            }

            if (!(villager.level() instanceof ServerLevel serverLevel)) {
                return List.of();
            }

            IVillageZone workplace = VillagesHelper.getWorkplaceZone(serverLevel, villager);
            if (workplace == null) {
                return List.of();
            }

            List<ItemStack> filter = workplace.getFilter();
            if (filter.isEmpty()) {
                return getProfessionDefault(villager);
            }

            return List.of(new WantedItem(stack -> {
                for (ItemStack wanted : filter) {
                    if (ItemStack.isSameItem(wanted, stack)) {
                        return true;
                    }
                }
                return false;
            }, 16, 0));
        }

        private List<IWantedItem> getProfessionDefault(Villager villager) {
            List<IWantedItem> resources = villager.getProfession()
                    .getAlwaysWantedItems(villager);

            if (!resources.isEmpty()) {
                return resources;
            }

            Brain<Villager> brain = villager.getBrain();
            if (!brain.hasMemoryValue(MemoryModuleTypes.CURRENT_RECIPE.get())) {
                return List.of();
            }

            ResourceLocation recipeId = villager.getBrain()
                    .getMemory(MemoryModuleTypes.CURRENT_RECIPE.get())
                    .orElse(null);
            if (recipeId == null) {
                return List.of();
            }

            Recipe<?> recipe = villager.level()
                    .getRecipeManager()
                    .byKey(recipeId)
                    .orElse(null);

            if (recipe == null) {
                return List.of();
            }

            Map<String, Pair<Ingredient, Integer>> ingredientMap = new HashMap<>();

            for (Ingredient ingredient : recipe.getIngredients()) {
                if (ingredient.isEmpty()) {
                    continue;
                }

                ItemStack[] items = ingredient.getItems();
                if (items.length == 0) {
                    continue;
                }

                String key = Arrays.stream(items)
                        .map(stack -> stack.getItem().toString())
                        .sorted()
                        .collect(Collectors.joining(","));

                if (key.isEmpty()) {
                    continue;
                }

                if (ingredientMap.containsKey(key)) {
                    Pair<Ingredient, Integer> pair = ingredientMap.get(key);
                    ingredientMap.put(key, Pair.of(pair.getFirst(), pair.getSecond() + 1));
                } else {
                    ingredientMap.put(key, Pair.of(ingredient, 1));
                }
            }

            List<IWantedItem> wantedItems = new ArrayList<>();
            SimpleContainer inventory = villager.getInventory();

            for (Pair<Ingredient, Integer> pair : ingredientMap.values()) {
                Ingredient ingredient = pair.getFirst();
                int neededCount = pair.getSecond();

                int inventoryCount = countIngredientInInventory(inventory, ingredient);

                if (inventoryCount < neededCount) {
                    wantedItems.add(new WantedItem(ingredient::test, neededCount, 0));
                }
            }

            return wantedItems;
        }

        private int countIngredientInInventory(SimpleContainer inventory, Ingredient ingredient) {
            int count = 0;
            for (int i = 0; i < inventory.getContainerSize(); i++) {
                ItemStack stack = inventory.getItem(i);
                if (!stack.isEmpty() && ingredient.test(stack)) {
                    count += stack.getCount();
                }
            }
            return count;
        }
    };

    public abstract List<IWantedItem> getFor(Villager villager);
}
