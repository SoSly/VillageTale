package org.sosly.villagetale.data;

import com.mojang.logging.LogUtils;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;
import org.sosly.villagetale.VillageTale;
import org.sosly.villagetale.data.loaders.EntityDataLoader;
import org.sosly.villagetale.data.loaders.ProfessionDataLoader;
import org.sosly.villagetale.data.loaders.ZoneTypeDataLoader;

@Mod.EventBusSubscriber(modid = VillageTale.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class DatapackReloadHandler {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final ProfessionDataLoader PROFESSION_DATA_LOADER = new ProfessionDataLoader();
    private static final ZoneTypeDataLoader ZONE_TYPE_DATA_LOADER = new ZoneTypeDataLoader();
    private static final EntityDataLoader ENTITY_DATA_LOADER = new EntityDataLoader();

    @SubscribeEvent
    public static void onAddReloadListener(AddReloadListenerEvent event) {
        LOGGER.info("Registering VillageTale datapack loaders");
        event.addListener(PROFESSION_DATA_LOADER);
        event.addListener(ZONE_TYPE_DATA_LOADER);
        event.addListener(ENTITY_DATA_LOADER);
    }

    public static void init() {
        LOGGER.info("Initializing VillageTale datapack system");
    }
}
