package org.sosly.villagetale.data;

import java.util.function.Predicate;
import net.minecraft.world.item.ItemStack;
import org.sosly.villagetale.api.data.IWantedItem;

public class WantedItem implements IWantedItem {
    private final Predicate<ItemStack> matcher;
    private final int amount;
    public WantedItem(Predicate<ItemStack> matcher, int amount) {
        this.matcher = matcher;
        this.amount = amount;
    }


    @Override
    public int getAmount() {
        return amount;
    }

    @Override
    public Predicate<ItemStack> getMatcher() {
        return matcher;
    }
}
