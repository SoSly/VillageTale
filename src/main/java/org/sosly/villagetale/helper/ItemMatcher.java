package org.sosly.villagetale.helper;

import net.minecraft.world.item.ItemStack;
import org.sosly.villagetale.api.data.IWantedItem;
import org.sosly.villagetale.data.WantedItem;
import org.sosly.villagetale.entity.Villager;

public enum ItemMatcher {
    PROFESSION_TOOL {
        @Override
        public WantedItem getFor(Villager villager) {
            IWantedItem tool = villager.getProfession().getTool().orElse(IWantedItem.EMPTY);
            if (tool instanceof WantedItem) {
                return (WantedItem) tool;
            }
            return (WantedItem) IWantedItem.EMPTY;
        }
    },
    FOOD {
        @Override
        public WantedItem getFor(Villager villager) {
            return new WantedItem(ItemStack::isEdible, 3);
        }
    },
    RESOURCES {
        @Override
        public WantedItem getFor(Villager villager) {
            // todo: add items from the current recipe or schematic the villager is working on
            IWantedItem resources = villager.getProfession().getAlwaysWantedItems().orElse(IWantedItem.EMPTY);
            if (resources instanceof WantedItem) {
                return (WantedItem) resources;
            }
            return (WantedItem) IWantedItem.EMPTY;
        }
    };

    public abstract WantedItem getFor(Villager villager);
}
