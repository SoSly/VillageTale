package org.sosly.villagetale.api.data;

import com.mojang.serialization.Codec;
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
    public static final IWantedItem EMPTY = new WantedItem(stack -> false, 0);
    
    /**
     * Codec for serializing and deserializing IWantedItem instances.
     * Uses the concrete WantedItem implementation for persistence while
     * maintaining the interface abstraction for API consumers.
     */
    public static final Codec<IWantedItem> CODEC = WantedItem.CODEC.xmap(
        wantedItem -> (IWantedItem) wantedItem,
        iWantedItem -> (WantedItem) iWantedItem
    );

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
