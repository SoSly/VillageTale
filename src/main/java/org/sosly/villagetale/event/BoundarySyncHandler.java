package org.sosly.villagetale.event;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import org.sosly.villagetale.VillageTale;
import org.sosly.villagetale.api.IVillageZone;
import org.sosly.villagetale.api.capability.IVillageCapability;
import org.sosly.villagetale.api.capability.IVillagesCapability;
import org.sosly.villagetale.capability.Capabilities;
import org.sosly.villagetale.data.VillageInfo;
import org.sosly.villagetale.network.NetworkHandler;
import org.sosly.villagetale.network.VillageBoundaryPacket;
import org.sosly.villagetale.network.ZoneBoundaryPacket;

@Mod.EventBusSubscriber(modid = VillageTale.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class BoundarySyncHandler {

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        syncBoundariesToPlayer(player);
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        syncBoundariesToPlayer(player);
    }

    private static void syncBoundariesToPlayer(ServerPlayer player) {
        ServerLevel level = player.serverLevel();

        level.getCapability(Capabilities.VILLAGES_CAPABILITY).ifPresent(villagesCapability -> {
            for (VillageInfo villageInfo : villagesCapability.getVillages()) {
                VillageBoundaryPacket packet = new VillageBoundaryPacket(
                    villageInfo.getVillageId(),
                    villageInfo.getVillageStartingChunk(),
                    villageInfo.getSquadius()
                );
                NetworkHandler.CHANNEL.send(
                    PacketDistributor.PLAYER.with(() -> player),
                    packet
                );

                level.getChunk(villageInfo.getVillageStartingChunk().x, villageInfo.getVillageStartingChunk().z)
                    .getCapability(Capabilities.VILLAGE_CAPABILITY)
                    .ifPresent(villageCapability -> syncZonesToPlayer(player, villageCapability));
            }
        });
    }

    private static void syncZonesToPlayer(ServerPlayer player, IVillageCapability villageCapability) {
        for (IVillageZone zone : villageCapability.getZones()) {
            if (zone.getShape() != null) {
                ZoneBoundaryPacket packet = zone.getShape().createBoundaryPacket(
                    zone.getUUID(),
                    villageCapability.getUUID()
                );
                if (packet != null) {
                    NetworkHandler.CHANNEL.send(
                        PacketDistributor.PLAYER.with(() -> player),
                        packet
                    );
                }
            }
        }
    }
}
