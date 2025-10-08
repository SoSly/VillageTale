package org.sosly.villagetale.item;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.sosly.villagetale.api.capability.IVillageCapability;
import org.sosly.villagetale.api.capability.IVillagesCapability;
import org.sosly.villagetale.block.TownHallBlock;
import org.sosly.villagetale.capability.Capabilities;
import org.sosly.villagetale.data.VillageInfo;
import org.sosly.villagetale.network.packets.clientbound.OpenVillageInfoScreen;
import org.sosly.villagetale.network.packets.clientbound.SyncVillageCapability;

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

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getLevel().isClientSide) {
            return InteractionResult.SUCCESS;
        }

        if (context.getLevel().getBlockState(context.getClickedPos()).getBlock() instanceof TownHallBlock) {
            return InteractionResult.PASS;
        }

        if (!(context.getLevel() instanceof ServerLevel serverLevel)) {
            return InteractionResult.FAIL;
        }

        if (!(context.getPlayer() instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.FAIL;
        }

        return openVillageInfoScreen(context.getItemInHand(), serverLevel, serverPlayer);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (level.isClientSide) {
            return InteractionResultHolder.success(stack);
        }

        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResultHolder.fail(stack);
        }

        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.fail(stack);
        }

        InteractionResult result = openVillageInfoScreen(stack, serverLevel, serverPlayer);
        if (result == InteractionResult.SUCCESS) {
            return InteractionResultHolder.success(stack);
        }

        return InteractionResultHolder.fail(stack);
    }

    private InteractionResult openVillageInfoScreen(ItemStack stack, ServerLevel serverLevel, ServerPlayer serverPlayer) {
        UUID villageId = getVillageUUID(stack);
        if (villageId == null) {
            serverPlayer.sendSystemMessage(Component.literal("This ledger is not linked to a village"));
            return InteractionResult.FAIL;
        }

        IVillagesCapability villagesCapability = serverLevel.getCapability(Capabilities.VILLAGES_CAPABILITY).orElse(null);
        if (villagesCapability == null) {
            return InteractionResult.FAIL;
        }

        VillageInfo village = villagesCapability.getVillageById(villageId);
        if (village == null) {
            serverPlayer.sendSystemMessage(Component.literal("This village no longer exists"));
            return InteractionResult.FAIL;
        }

        ChunkPos villageChunk = village.getVillageStartingChunk();
        IVillageCapability villageCapability = serverLevel.getChunk(villageChunk.x, villageChunk.z)
                .getCapability(Capabilities.VILLAGE_CAPABILITY).orElse(null);

        if (villageCapability == null) {
            return InteractionResult.FAIL;
        }

        if (!villageCapability.hasPermission(serverPlayer.getUUID(), IVillageCapability.Permission.OWNER)) {
            serverPlayer.sendSystemMessage(Component.literal("You do not have permission to view this village"));
            return InteractionResult.FAIL;
        }

        SyncVillageCapability.send(serverPlayer, villageCapability, serverLevel.getServer());
        OpenVillageInfoScreen.send(serverPlayer, villageId);

        return InteractionResult.SUCCESS;
    }
}
