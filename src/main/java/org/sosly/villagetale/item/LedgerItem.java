package org.sosly.villagetale.item;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.sosly.villagetale.api.capability.IVillageCapability;
import org.sosly.villagetale.api.capability.IVillagesCapability;
import org.sosly.villagetale.capability.Capabilities;
import org.sosly.villagetale.data.VillageInfo;
import org.sosly.villagetale.entity.MemoryModuleTypes;
import org.sosly.villagetale.entity.Villager;
import org.sosly.villagetale.event.VillagerInteractionHandler;
import org.sosly.villagetale.network.packets.clientbound.OpenVillageInfoScreen;
import org.sosly.villagetale.network.packets.clientbound.OpenVillagerConversionScreen;
import org.sosly.villagetale.network.packets.clientbound.OpenVillagerManagementScreen;
import org.sosly.villagetale.network.packets.clientbound.SyncVillageCapability;

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

        if (!(context.getLevel() instanceof ServerLevel serverLevel)) {
            return InteractionResult.FAIL;
        }

        if (!(context.getPlayer() instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.FAIL;
        }

        return openVillageInfoScreen(context.getItemInHand(), serverLevel, serverPlayer);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (player.level().isClientSide) {
            return InteractionResult.SUCCESS;
        }

        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.FAIL;
        }

        // CHECKSTYLE:OFF RegexpSingleline - legitimate FQCN due to name collision with org.sosly.villagetale.entity.Villager
        if (target instanceof net.minecraft.world.entity.npc.Villager vanillaVillager) {
        // CHECKSTYLE:ON
            vanillaVillager.getNavigation().stop();
            vanillaVillager.lookAt(player, 180.0F, 180.0F);
            VillagerInteractionHandler.addVillagerToConversation(target.getId());
            OpenVillagerConversionScreen.send(serverPlayer, target.getId());
            return InteractionResult.CONSUME;
        }

        if (!(target instanceof Villager villager)) {
            return InteractionResult.PASS;
        }

        villager.getNavigation().stop();
        villager.lookAt(player, 180.0F, 180.0F);
        VillagerInteractionHandler.addVillagerToConversation(target.getId());

        if (villager.getVillage().isEmpty()) {
            return InteractionResult.FAIL;
        }

        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return InteractionResult.FAIL;
        }

        IVillagesCapability villagesCapability = serverLevel.getCapability(Capabilities.VILLAGES_CAPABILITY).orElse(null);
        if (villagesCapability == null) {
            return InteractionResult.FAIL;
        }

        VillageInfo village = villagesCapability.getVillageById(villager.getVillage().get());
        if (village == null) {
            return InteractionResult.FAIL;
        }

        ChunkPos villageChunk = village.getVillageStartingChunk();
        IVillageCapability villageCapability = serverLevel.getChunk(villageChunk.x, villageChunk.z)
            .getCapability(Capabilities.VILLAGE_CAPABILITY).orElse(null);

        if (villageCapability == null) {
            return InteractionResult.FAIL;
        }

        UUID homeZoneId = villager.getBrain().getMemory(MemoryModuleTypes.HOME_ZONE.get()).orElse(null);
        UUID workZoneId = villager.getBrain().getMemory(MemoryModuleTypes.WORK_ZONE.get()).orElse(null);

        List<ItemStack> inventory = new ArrayList<>();
        for (int i = 0; i < villager.getInventory().getContainerSize(); i++) {
            inventory.add(villager.getInventory().getItem(i));
        }

        for (ItemStack equipmentStack : villager.getHandSlots()) {
            if (!equipmentStack.isEmpty()) {
                inventory.add(equipmentStack);
            }
        }

        for (ItemStack armorStack : villager.getArmorSlots()) {
            if (!armorStack.isEmpty()) {
                inventory.add(armorStack);
            }
        }

        float health = villager.getHealth();
        int hunger = villager.getFoodData().getFoodLevel();

        List<ResourceLocation> knownRecipes = new ArrayList<>(villager.getRecipeKnowledge().known());

        SyncVillageCapability.send(serverPlayer, villageCapability, serverLevel.getServer());
        OpenVillagerManagementScreen.send(serverPlayer, target.getId(), villager.getVillage().get(), homeZoneId, workZoneId, inventory, health, hunger, knownRecipes);
        return InteractionResult.CONSUME;
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
