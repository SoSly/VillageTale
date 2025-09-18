package org.sosly.villageworks.api.data;

import java.util.function.Predicate;
import net.minecraft.world.item.ItemStack;
import org.sosly.villageworks.data.WantedItem;

/**
 * Represents an immutable specification for items a villager wants to acquire.
 * Defines both the matching criteria and desired quantity.
 */
public interface IWantedItem {
    public static final IWantedItem EMPTY = new WantedItem(stack -> false, 0);

    /**
     * Gets the desired quantity of this item.
     * @return The amount wanted
     */
    public int getAmount();

    /**
     * Gets the predicate used to match desired items.
     * @return The item matching predicate
     */
    public Predicate<ItemStack> getMatcher();

}
