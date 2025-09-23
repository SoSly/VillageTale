package org.sosly.villagetale.helper;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import org.sosly.villagetale.api.IVillageZone;
import org.sosly.villagetale.api.IWantedItem;
import org.sosly.villagetale.data.WantedItem;
import org.sosly.villagetale.entity.MemoryModuleTypes;
import org.sosly.villagetale.entity.Villager;

import java.util.List;

public enum ItemMatcher {
    PROFESSION_TOOL {
        @Override
        public IWantedItem getFor(Villager villager) {
            IWantedItem tool = villager.getProfession().getTool().orElse(IWantedItem.EMPTY);
            if (tool instanceof WantedItem) {
                return tool;
            }
            return  IWantedItem.EMPTY;
        }
    },
    FOOD {
        @Override
        public IWantedItem getFor(Villager villager) {
            return new WantedItem(ItemStack::isEdible, 3, 0);
        }
    },
    RESOURCES {
        @Override
        public IWantedItem getFor(Villager villager) {
            if (!villager.getBrain().hasMemoryValue(MemoryModuleTypes.VILLAGE.get())) {
                return IWantedItem.EMPTY;
            }

            if (!(villager.level() instanceof ServerLevel serverLevel)) {
                return IWantedItem.EMPTY;
            }

            IVillageZone workplace = VillagesHelper.getWorkplaceZone(serverLevel, villager);
            if (workplace == null) {
                return IWantedItem.EMPTY;
            }

            List<ItemStack> filter = workplace.getFilter();
            if (filter.isEmpty()) {
                return getProfessionDefault(villager);
            }

            return new WantedItem(stack -> {
                for (ItemStack wanted : filter) {
                    if (ItemStack.isSameItem(wanted, stack)) {
                        return true;
                    }
                }
                return false;
            }, 16, 0);
        }

        private IWantedItem getProfessionDefault(Villager villager) {
            IWantedItem resources = villager.getProfession()
                    .getAlwaysWantedItems()
                    .orElse(null);

            if (resources != null) {
                return resources;
            }

            Brain<Villager> brain = villager.getBrain();
            if (!brain.hasMemoryValue(MemoryModuleTypes.CURRENT_RECIPE.get())) {
                return IWantedItem.EMPTY;
            }

            ResourceLocation recipeId = villager.getBrain()
                    .getMemory(MemoryModuleTypes.CURRENT_RECIPE.get())
                    .orElse(null);
            if (recipeId == null) {
                return IWantedItem.EMPTY;
            }

            Recipe<?> recipe = villager.level()
                    .getRecipeManager()
                    .byKey(recipeId)
                    .orElse(null);

            if (recipe == null) {
                return IWantedItem.EMPTY;
            }

            return new WantedItem((item) -> recipe.getIngredients()
                    .stream()
                    .anyMatch(ingredient -> ingredient.test(item)), 9, 9);
        }
    };

    public abstract IWantedItem getFor(Villager villager);
}
