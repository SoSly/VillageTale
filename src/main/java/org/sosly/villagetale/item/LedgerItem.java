package org.sosly.villagetale.item;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class LedgerItem extends Item {
    private static final String VILLAGE_UUID_KEY = "VillageUUID";

    public LedgerItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean hasCraftingRemainingItem(ItemStack stack) {
        return true;
    }

    @Override
    public ItemStack getCraftingRemainingItem(ItemStack stack) {
        return stack.copy();
    }

    @Nullable
    public static UUID getVillageUUID(ItemStack stack) {
        if (!stack.hasTag()) {
            return null;
        }

        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.hasUUID(VILLAGE_UUID_KEY)) {
            return null;
        }

        return tag.getUUID(VILLAGE_UUID_KEY);
    }

    public static void setVillageUUID(ItemStack stack, @Nullable UUID villageUUID) {
        if (villageUUID == null) {
            if (stack.hasTag()) {
                CompoundTag tag = stack.getTag();
                if (tag != null) {
                    tag.remove(VILLAGE_UUID_KEY);
                }
            }
            return;
        }

        stack.getOrCreateTag().putUUID(VILLAGE_UUID_KEY, villageUUID);
    }

    public static boolean hasVillageLink(ItemStack stack) {
        return getVillageUUID(stack) != null;
    }
}
