package org.sosly.villageworks.event;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.sosly.villageworks.VillageWorks;
import org.sosly.villageworks.renderer.VillagerRenderer;
import org.sosly.villageworks.registry.EntityTypes;

@Mod.EventBusSubscriber(modid = VillageWorks.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientEntities {

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(EntityTypes.VILLAGER.get(), VillagerRenderer::new);
    }
}
