package org.sosly.villagetale.network.packets.serverbound;

import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.network.NetworkEvent;
import org.sosly.villagetale.VillageTale;
import org.sosly.villagetale.entity.EntityTypes;
import org.sosly.villagetale.entity.Villager;
import org.sosly.villagetale.network.BasePacket;
import org.sosly.villagetale.network.NetworkHandler;
import org.sosly.villagetale.network.ServerPacketHandler;

public class ConvertVillager extends BasePacket {
    private static final int EMERALD_COST = 10;

    private final int villagerEntityId;

    private ConvertVillager(int villagerEntityId) {
        this.villagerEntityId = villagerEntityId;
    }

    public static void send(int villagerEntityId) {
        ConvertVillager packet = new ConvertVillager(villagerEntityId);
        NetworkHandler.CHANNEL.sendToServer(packet);
    }

    public static void encode(ConvertVillager msg, FriendlyByteBuf buffer) {
        buffer.writeInt(msg.villagerEntityId);
    }

    public static ConvertVillager decode(FriendlyByteBuf buffer) {
        ConvertVillager msg;

        try {
            int villagerEntityId = buffer.readInt();
            msg = new ConvertVillager(villagerEntityId);
        } catch (IndexOutOfBoundsException | IllegalArgumentException err) {
            VillageTale.LOGGER.error("Exception while reading ConvertVillager: {}", err.toString());
            return null;
        }

        msg.messageIsValid = true;
        return msg;
    }

    public static void handle(ConvertVillager msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        if (!ServerPacketHandler.validateBasics(msg, context)) {
            return;
        }

        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }

            ServerLevel level = player.serverLevel();
            Entity entity = level.getEntity(msg.villagerEntityId);

            if (!(entity instanceof Villager vanillaVillager)) {
                player.sendSystemMessage(Component.literal("Invalid villager entity"));
                return;
            }

            if (!hasEnoughEmeralds(player)) {
                player.sendSystemMessage(Component.translatable("villagetale.conversion.insufficient_emeralds", EMERALD_COST));
                return;
            }

            if (!deductEmeralds(player)) {
                player.sendSystemMessage(Component.literal("Failed to deduct emeralds"));
                return;
            }

            Villager vtVillager = EntityTypes.VILLAGER.get().create(level);
            if (vtVillager == null) {
                player.sendSystemMessage(Component.literal("Failed to create VillageTale villager"));
                refundEmeralds(player);
                return;
            }

            vtVillager.moveTo(vanillaVillager.getX(), vanillaVillager.getY(), vanillaVillager.getZ(),
                vanillaVillager.getYRot(), vanillaVillager.getXRot());

            if (vanillaVillager.hasCustomName()) {
                vtVillager.setCustomName(vanillaVillager.getCustomName());
            }

            vtVillager.setFollowingPlayer(player.getUUID());

            level.addFreshEntity(vtVillager);
            vanillaVillager.discard();

            player.sendSystemMessage(Component.translatable("villagetale.conversion.success"));
        });
    }

    private static boolean hasEnoughEmeralds(ServerPlayer player) {
        int emeraldCount = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(Items.EMERALD)) {
                emeraldCount += stack.getCount();
                if (emeraldCount >= EMERALD_COST) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean deductEmeralds(ServerPlayer player) {
        int remaining = EMERALD_COST;
        for (int i = 0; i < player.getInventory().items.size(); i++) {
            ItemStack stack = player.getInventory().items.get(i);
            if (stack.is(Items.EMERALD)) {
                int toRemove = Math.min(remaining, stack.getCount());
                stack.shrink(toRemove);
                remaining -= toRemove;

                if (remaining == 0) {
                    return true;
                }
            }
        }
        return false;
    }

    private static void refundEmeralds(ServerPlayer player) {
        ItemStack emeralds = new ItemStack(Items.EMERALD, EMERALD_COST);
        player.addItem(emeralds);
    }

    public int getVillagerEntityId() {
        return villagerEntityId;
    }
}
