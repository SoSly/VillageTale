package org.sosly.villagetale.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import org.sosly.villagetale.VillageTale;
import org.sosly.villagetale.network.packets.clientbound.OpenVillageInfoScreen;
import org.sosly.villagetale.network.packets.clientbound.OpenVillagerConversionScreen;
import org.sosly.villagetale.network.packets.clientbound.OpenVillagerManagementScreen;
import org.sosly.villagetale.network.packets.clientbound.RemoveZoneBoundary;
import org.sosly.villagetale.network.packets.clientbound.SyncVillageCapability;
import org.sosly.villagetale.network.packets.clientbound.VillageBoundary;
import org.sosly.villagetale.network.packets.clientbound.VillagerEquipmentSync;
import org.sosly.villagetale.network.packets.clientbound.VillagerProfessionSync;
import org.sosly.villagetale.network.packets.clientbound.ZoneBoundary;
import org.sosly.villagetale.network.packets.serverbound.ConvertVillager;
import org.sosly.villagetale.network.packets.serverbound.CreateZone;
import org.sosly.villagetale.network.packets.serverbound.DeleteZone;
import org.sosly.villagetale.network.packets.serverbound.SetZoneCreationMode;
import org.sosly.villagetale.network.packets.serverbound.UpdateVillageInfo;
import org.sosly.villagetale.network.packets.serverbound.UpdateVillagerAssignment;
import org.sosly.villagetale.network.packets.serverbound.UpdateZoneFilters;
import org.sosly.villagetale.network.packets.serverbound.UpdateZoneName;

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

        CHANNEL.registerMessage(packetId++, OpenVillagerConversionScreen.class,
                OpenVillagerConversionScreen::encode, OpenVillagerConversionScreen::decode, OpenVillagerConversionScreen::handle);

        CHANNEL.registerMessage(packetId++, ConvertVillager.class,
                ConvertVillager::encode, ConvertVillager::decode, ConvertVillager::handle);

        CHANNEL.registerMessage(packetId++, SyncVillageCapability.class,
                SyncVillageCapability::encode, SyncVillageCapability::decode, SyncVillageCapability::handle);

        CHANNEL.registerMessage(packetId++, OpenVillageInfoScreen.class,
                OpenVillageInfoScreen::encode, OpenVillageInfoScreen::decode, OpenVillageInfoScreen::handle);

        CHANNEL.registerMessage(packetId++, UpdateZoneName.class,
                UpdateZoneName::encode, UpdateZoneName::decode, UpdateZoneName::handle);

        CHANNEL.registerMessage(packetId++, UpdateZoneFilters.class,
                UpdateZoneFilters::encode, UpdateZoneFilters::decode, UpdateZoneFilters::handle);

        CHANNEL.registerMessage(packetId++, DeleteZone.class,
                DeleteZone::encode, DeleteZone::decode, DeleteZone::handle);

        CHANNEL.registerMessage(packetId++, RemoveZoneBoundary.class,
                RemoveZoneBoundary::encode, RemoveZoneBoundary::decode, RemoveZoneBoundary::handle);

        CHANNEL.registerMessage(packetId++, SetZoneCreationMode.class,
                SetZoneCreationMode::encode, SetZoneCreationMode::decode, SetZoneCreationMode::handle);

        CHANNEL.registerMessage(packetId++, CreateZone.class,
                CreateZone::encode, CreateZone::decode, CreateZone::handle);

        CHANNEL.registerMessage(packetId++, OpenVillagerManagementScreen.class,
                OpenVillagerManagementScreen::encode, OpenVillagerManagementScreen::decode, OpenVillagerManagementScreen::handle);

        CHANNEL.registerMessage(packetId++, UpdateVillagerAssignment.class,
                UpdateVillagerAssignment::encode, UpdateVillagerAssignment::decode, UpdateVillagerAssignment::handle);

        VillageTale.LOGGER.info("VillageTale registered {} network messages", packetId);
    }
}
