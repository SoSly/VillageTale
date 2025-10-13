package org.sosly.villagetale.api;

import java.util.function.Predicate;
import net.minecraft.world.item.ItemStack;
import org.sosly.villagetale.data.WantedItem;

/**
 * Represents an immutable specification for items a villager wants to acquire.
 * Defines both the matching criteria and desired quantity.
 */
public interface IWantedItem {
    /**
     * An empty wanted item that matches nothing and wants zero quantity.
     * Used as a default value when no specific item is needed.
     */
    IWantedItem EMPTY = new WantedItem(stack -> false, 0, 0);


    /**
     * Gets the desired quantity of this item to fetch from storage.
     * @return The amount to fetch when needed
     */
    int getAmount();

    /**
     * Gets the minimum threshold that triggers fetching this item.
     * @return The minimum count below which fetching is triggered
     */
    int getMinimum();

    /**
     * Gets the predicate used to match desired items.
     * @return The item matching predicate
     */
    Predicate<ItemStack> getMatcher();

}
