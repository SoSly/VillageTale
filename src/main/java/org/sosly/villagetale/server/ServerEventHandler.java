package org.sosly.villagetale.server;

import net.minecraft.world.InteractionResult;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.sosly.villagetale.VillageTale;
import org.sosly.villagetale.helper.ZoneCreationHelper;

@Mod.EventBusSubscriber(modid = VillageTale.MOD_ID)
public class ServerEventHandler {

    @SubscribeEvent
    public static void onPlayerLogout(PlayerLoggedOutEvent event) {
        ZoneCreationHelper.getInstance().removePlayer(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getLevel().isClientSide) {
            return;
        }

        if (ZoneCreationHelper.getInstance().isInCreationMode(event.getEntity().getUUID())) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.CONSUME);
        }
    }
}
