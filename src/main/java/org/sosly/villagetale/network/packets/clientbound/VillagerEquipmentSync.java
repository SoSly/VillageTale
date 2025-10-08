package org.sosly.villagetale.network.packets.clientbound;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;
import org.sosly.villagetale.VillageTale;
import org.sosly.villagetale.entity.Villager;
import org.sosly.villagetale.network.BasePacket;
import org.sosly.villagetale.network.ClientPacketHandler;
import org.sosly.villagetale.network.NetworkHandler;

import java.util.function.Supplier;

public class VillagerEquipmentSync extends BasePacket {
    private final int entityId;
    private final InteractionHand hand;
    private final ItemStack itemStack;

    private VillagerEquipmentSync(int entityId, InteractionHand hand, ItemStack itemStack) {
        this.entityId = entityId;
        this.hand = hand;
        this.itemStack = itemStack.copy();
    }

    public static void sendToNearbyPlayers(Villager villager, InteractionHand hand, ItemStack itemStack) {
        VillagerEquipmentSync packet = new VillagerEquipmentSync(villager.getId(), hand, itemStack);
        NetworkHandler.CHANNEL.send(PacketDistributor.TRACKING_ENTITY.with(() -> villager), packet);
    }

    public static void encode(VillagerEquipmentSync msg, FriendlyByteBuf buffer) {
        buffer.writeVarInt(msg.entityId);
        buffer.writeEnum(msg.hand);
        buffer.writeItem(msg.itemStack);
    }

    public static VillagerEquipmentSync decode(FriendlyByteBuf buffer) {
        VillagerEquipmentSync msg;

        try {
            int entityId = buffer.readVarInt();
            InteractionHand hand = buffer.readEnum(InteractionHand.class);
            ItemStack itemStack = buffer.readItem();
            msg = new VillagerEquipmentSync(entityId, hand, itemStack);
        } catch (IndexOutOfBoundsException | IllegalArgumentException err) {
            VillageTale.LOGGER.error("Exception while reading VillagerEquipmentSync: {}", err.toString());
            return null;
        }

        msg.messageIsValid = true;
        return msg;
    }

    public static void handle(VillagerEquipmentSync msg, Supplier<NetworkEvent.Context> ctx) {
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
