package org.sosly.villagetale.data;

import java.util.function.Predicate;
import net.minecraft.world.item.ItemStack;
import org.sosly.villagetale.api.IWantedItem;

public class WantedItem implements IWantedItem {
    private final Predicate<ItemStack> matcher;
    private final int amount;
    private final int minimum;

    public WantedItem(Predicate<ItemStack> matcher, int amount, int minimum) {
        this.matcher = matcher;
        this.amount = amount;
        this.minimum = minimum;
    }

    @Override
    public int getAmount() {
        return amount;
    }

    @Override
    public int getMinimum() {
        return minimum;
    }

    @Override
    public Predicate<ItemStack> getMatcher() {
        return matcher;
    }
}
