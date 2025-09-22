package org.sosly.villagetale.helper;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import org.sosly.villagetale.api.IVillageZone;
import org.sosly.villagetale.api.IWantedItem;
import org.sosly.villagetale.data.WantedItem;
import org.sosly.villagetale.entity.Villager;

import java.util.List;

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
            return new WantedItem(ItemStack::isEdible, 3, 0);
        }
    },
    RESOURCES {
        @Override
        public WantedItem getFor(Villager villager) {
            if (!(villager.level() instanceof ServerLevel serverLevel)) {
                return getProfessionDefault(villager);
            }

            IVillageZone workplace = VillagesHelper.getWorkplaceZone(serverLevel, villager);
            if (workplace == null) {
                return getProfessionDefault(villager);
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

        private WantedItem getProfessionDefault(Villager villager) {
            IWantedItem resources = villager.getProfession().getAlwaysWantedItems().orElse(IWantedItem.EMPTY);
            if (resources instanceof WantedItem wanted) {
                return wanted;
            }
            return (WantedItem) IWantedItem.EMPTY;
        }
    };

    public abstract WantedItem getFor(Villager villager);
}
