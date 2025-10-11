package org.sosly.villagetale.helper;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import org.sosly.villagetale.api.IVillageZone;
import org.sosly.villagetale.api.IWantedItem;
import org.sosly.villagetale.compat.CompatRegistry;
import org.sosly.villagetale.data.ItemOrTagMatcher;
import org.sosly.villagetale.data.WantedItem;
import org.sosly.villagetale.entity.MemoryModuleTypes;
import org.sosly.villagetale.entity.Villager;

import java.util.ArrayList;
import java.util.List;

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

            int ingredientCount = (int) recipe.getIngredients().stream()
                    .filter(ingredient -> !ingredient.isEmpty())
                    .count();

            int minimum = Math.max(0, ingredientCount - 1);
            int target = Math.max(ingredientCount * 3, 9);

            List<IWantedItem> wantedItems = new ArrayList<>();
            wantedItems.add(new WantedItem((item) -> recipe.getIngredients()
                    .stream()
                    .anyMatch(ingredient -> ingredient.test(item)), target, minimum));

            ItemOrTagMatcher fuelMatcher = CompatRegistry.getRecipeManager()
                    .getFuelItems(recipe)
                    .orElse(null);
            if (fuelMatcher != null) {
                wantedItems.add(new WantedItem(fuelMatcher::matches, 3, 0));
            }

            return wantedItems;
        }
    };

    public abstract List<IWantedItem> getFor(Villager villager);
}
