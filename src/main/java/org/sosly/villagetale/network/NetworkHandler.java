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
import org.sosly.villagetale.network.packets.clientbound.OpenTownHallScreen;
import org.sosly.villagetale.network.packets.clientbound.OpenVillagerConversionScreen;
import org.sosly.villagetale.network.packets.clientbound.VillageBoundary;
import org.sosly.villagetale.network.packets.clientbound.VillagerEquipmentSync;
import org.sosly.villagetale.network.packets.clientbound.VillagerProfessionSync;
import org.sosly.villagetale.network.packets.clientbound.ZoneBoundary;
import org.sosly.villagetale.network.packets.serverbound.ConvertVillager;
import org.sosly.villagetale.network.packets.serverbound.UpdateVillageInfo;

public class NetworkHandler {
    private static final String PROTOCOL_VERSION = "1";
    private static final ResourceLocation CHANNEL_NAME = new ResourceLocation(VillageTale.MOD_ID, "main");

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(CHANNEL_NAME, () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals, PROTOCOL_VERSION::equals);

    private static int packetId = 0;

    public static void init() {
        CHANNEL.registerMessage(packetId++, VillagerProfessionSync.class,
                VillagerProfessionSync::encode, VillagerProfessionSync::decode, VillagerProfessionSync::handle);

        CHANNEL.registerMessage(packetId++, VillagerEquipmentSync.class,
                VillagerEquipmentSync::encode, VillagerEquipmentSync::decode, VillagerEquipmentSync::handle
        );

        CHANNEL.registerMessage(packetId++, VillageBoundary.class,
                VillageBoundary::encode, VillageBoundary::decode, VillageBoundary::handle);

        CHANNEL.registerMessage(packetId++, ZoneBoundary.class,
                ZoneBoundary::encode, ZoneBoundary::decode, ZoneBoundary::handle);

        CHANNEL.registerMessage(packetId++, UpdateVillageInfo.class,
                UpdateVillageInfo::encode, UpdateVillageInfo::decode, UpdateVillageInfo::handle);

        CHANNEL.registerMessage(packetId++, OpenTownHallScreen.class,
                OpenTownHallScreen::encode, OpenTownHallScreen::decode, OpenTownHallScreen::handle);

        CHANNEL.registerMessage(packetId++, OpenVillagerConversionScreen.class,
                OpenVillagerConversionScreen::encode, OpenVillagerConversionScreen::decode, OpenVillagerConversionScreen::handle);

        CHANNEL.registerMessage(packetId++, ConvertVillager.class,
                ConvertVillager::encode, ConvertVillager::decode, ConvertVillager::handle);

        VillageTale.LOGGER.info("VillageTale registered {} network messages", packetId);
    }

    public static void syncProfessionToPlayer(Villager villager, ServerPlayer player) {
        ResourceLocation professionId = villager.getProfession().getID();
        VillagerProfessionSync packet = new VillagerProfessionSync(villager.getId(), professionId);
        CHANNEL.sendTo(packet, player.connection.connection, NetworkDirection.PLAY_TO_CLIENT);
    }

    public static void syncProfessionToNearbyPlayers(Villager villager) {
        ResourceLocation professionId = villager.getProfession().getID();
        VillagerProfessionSync packet = new VillagerProfessionSync(villager.getId(), professionId);
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY.with(() -> villager), packet);
    }

    public static void syncEquipmentToNearbyPlayers(Villager villager, InteractionHand hand, ItemStack itemStack) {
        VillagerEquipmentSync packet = new VillagerEquipmentSync(villager.getId(), hand, itemStack);
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY.with(() -> villager), packet);
    }
}
