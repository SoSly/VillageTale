package org.sosly.villagetale.network.packets.clientbound;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import org.sosly.villagetale.VillageTale;
import org.sosly.villagetale.network.BasePacket;
import org.sosly.villagetale.network.ClientPacketHandler;

import java.util.function.Supplier;

public class VillagerEquipmentSyncPacket extends BasePacket {
    private final int entityId;
    private final InteractionHand hand;
    private final ItemStack itemStack;

    public VillagerEquipmentSyncPacket(int entityId, InteractionHand hand, ItemStack itemStack) {
        this.entityId = entityId;
        this.hand = hand;
        this.itemStack = itemStack.copy();
    }

    public static void encode(VillagerEquipmentSyncPacket msg, FriendlyByteBuf buffer) {
        buffer.writeVarInt(msg.entityId);
        buffer.writeEnum(msg.hand);
        buffer.writeItem(msg.itemStack);
    }

    public static VillagerEquipmentSyncPacket decode(FriendlyByteBuf buffer) {
        VillagerEquipmentSyncPacket msg;

        try {
            int entityId = buffer.readVarInt();
            InteractionHand hand = buffer.readEnum(InteractionHand.class);
            ItemStack itemStack = buffer.readItem();
            msg = new VillagerEquipmentSyncPacket(entityId, hand, itemStack);
        } catch (IndexOutOfBoundsException | IllegalArgumentException err) {
            VillageTale.LOGGER.error("Exception while reading VillagerEquipmentSyncPacket: {}", err.toString());
            return null;
        }

        msg.messageIsValid = true;
        return msg;
    }

    public static void handle(VillagerEquipmentSyncPacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        if (ClientPacketHandler.validateBasics(msg, context)) {
            context.enqueueWork(() -> {
                Minecraft mc = Minecraft.getInstance();
                if (mc.level == null) {
                    return;
                }

                Entity entity = mc.level.getEntity(msg.entityId);
                if (entity instanceof LivingEntity livingEntity) {
                    livingEntity.setItemInHand(msg.hand, msg.itemStack);
                }
            });
        }
    }

    public int getEntityId() {
        return entityId;
    }

    public InteractionHand getHand() {
        return hand;
    }

    public ItemStack getItemStack() {
        return itemStack;
    }
}
