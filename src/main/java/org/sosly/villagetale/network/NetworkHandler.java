package org.sosly.villagetale.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import org.sosly.villagetale.VillageTale;
import org.sosly.villagetale.entity.Villager;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

public class NetworkHandler {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
        new ResourceLocation(VillageTale.MOD_ID, "main"),
        () -> PROTOCOL_VERSION,
        PROTOCOL_VERSION::equals,
        PROTOCOL_VERSION::equals
    );

    private static int packetId = 0;

    public static void init() {
        CHANNEL.registerMessage(
            packetId++,
            VillagerProfessionSyncPacket.class,
            VillagerProfessionSyncPacket::encode,
            VillagerProfessionSyncPacket::decode,
            VillagerProfessionSyncPacket::handle
        );
        
        CHANNEL.registerMessage(
            packetId++,
            VillagerEquipmentSyncPacket.class,
            VillagerEquipmentSyncPacket::encode,
            VillagerEquipmentSyncPacket::decode,
            VillagerEquipmentSyncPacket::handle
        );
    }

    public static void syncProfessionToPlayer(Villager villager, ServerPlayer player) {
        ResourceLocation professionId = villager.getProfession().getID();
        VillagerProfessionSyncPacket packet = new VillagerProfessionSyncPacket(villager.getId(), professionId);
        CHANNEL.sendTo(packet, player.connection.connection, NetworkDirection.PLAY_TO_CLIENT);
    }

    public static void syncProfessionToNearbyPlayers(Villager villager) {
        ResourceLocation professionId = villager.getProfession().getID();
        VillagerProfessionSyncPacket packet = new VillagerProfessionSyncPacket(villager.getId(), professionId);
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY.with(() -> villager), packet);
    }
    
    public static void syncEquipmentToNearbyPlayers(Villager villager, InteractionHand hand, ItemStack itemStack) {
        VillagerEquipmentSyncPacket packet = new VillagerEquipmentSyncPacket(villager.getId(), hand, itemStack);
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY.with(() -> villager), packet);
    }
}