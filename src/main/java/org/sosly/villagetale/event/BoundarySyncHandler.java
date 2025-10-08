package org.sosly.villagetale.event;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.sosly.villagetale.VillageTale;
import org.sosly.villagetale.api.IVillageZone;
import org.sosly.villagetale.api.capability.IVillageCapability;
import org.sosly.villagetale.api.capability.IVillagesCapability;
import org.sosly.villagetale.capability.Capabilities;
import org.sosly.villagetale.data.VillageInfo;
import org.sosly.villagetale.helper.VillagesHelper;
import org.sosly.villagetale.network.packets.clientbound.VillageBoundary;
import org.sosly.villagetale.network.packets.clientbound.ZoneBoundary;

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

        IVillagesCapability villages = level.getCapability(Capabilities.VILLAGES_CAPABILITY).orElse(null);
        if (villages == null) {
            return;
        }

        for (VillageInfo villageInfo : villages.getVillages()) {
            VillageBoundary.send(level, villageInfo.getVillageId(),
                    villageInfo.getVillageStartingChunk(), villageInfo.getSquadius());

            IVillageCapability village = VillagesHelper.getVillageCapability(level, villageInfo.getVillageId());
            if (village == null) {
                continue;
            }

            for (IVillageZone zone : village.getZones()) {
                ZoneBoundary.sendToPlayer(player, zone.getUUID(), village.getUUID(), zone.getShape());
            }
        }
    }
}
