package org.sosly.villagetale.helper;

import net.minecraft.world.item.ItemStack;
import org.sosly.villagetale.api.data.IWantedItem;
import org.sosly.villagetale.data.WantedItem;
import org.sosly.villagetale.entity.Villager;

public enum ItemMatcher {
    PROFESSION_TOOL {
        @Override
        public IWantedItem getFor(Villager villager) {
            return villager.getProfession().getTool().orElse(IWantedItem.EMPTY);
        }
    },
    FOOD {
        @Override
        public IWantedItem getFor(Villager villager) {
            return new WantedItem(ItemStack::isEdible, 3);
        }
    },
    RESOURCES {
        @Override
        public IWantedItem getFor(Villager villager) {
            // todo: add items from the current recipe or schematic the villager is working on
            return villager.getProfession().getAlwaysWantedItems().orElse(IWantedItem.EMPTY);
        }
    };

    public abstract IWantedItem getFor(Villager villager);
}
