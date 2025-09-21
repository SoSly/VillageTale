package org.sosly.villagetale.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public class VillagerEquipmentSyncPacket {
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
        int entityId = buffer.readVarInt();
        InteractionHand hand = buffer.readEnum(InteractionHand.class);
        ItemStack itemStack = buffer.readItem();
        return new VillagerEquipmentSyncPacket(entityId, hand, itemStack);
    }

    public static void handle(VillagerEquipmentSyncPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> ClientPacketHandler.handleEquipmentSync(msg));
        ctx.get().setPacketHandled(true);
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