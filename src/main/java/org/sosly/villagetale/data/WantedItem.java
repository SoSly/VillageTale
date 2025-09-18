package org.sosly.villagetale.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.function.Predicate;
import net.minecraft.world.item.ItemStack;
import org.sosly.villagetale.api.data.IWantedItem;

public class WantedItem implements IWantedItem {
    private final Predicate<ItemStack> matcher;
    private final int amount;
    
    public static final Codec<WantedItem> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            Codec.INT.fieldOf("amount").forGetter(WantedItem::getAmount)
        ).apply(instance, amount -> new WantedItem(stack -> false, amount))
    );
    
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
