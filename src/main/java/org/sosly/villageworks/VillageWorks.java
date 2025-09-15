package org.sosly.villageworks;

import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import org.sosly.villageworks.command.VillageWorksCommand;
import org.sosly.villageworks.entity.Villager;
import org.sosly.villageworks.registry.EntityTypes;
import org.sosly.villageworks.registry.MemoryModuleTypes;
import org.sosly.villageworks.registry.SensorTypes;

@Mod(VillageWorks.MOD_ID)
public class VillageWorks {
    public static final String MOD_ID = "villageworks";
    public static final Logger LOGGER = LogUtils.getLogger();

    public VillageWorks() {
        LOGGER.info("Loading VillageWorks");

        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        EntityTypes.register(modEventBus);
        MemoryModuleTypes.register(modEventBus);
        SensorTypes.register(modEventBus);

        modEventBus.addListener(this::setup);
        modEventBus.addListener(this::onEntityAttributeCreation);

        MinecraftForge.EVENT_BUS.register(this);
    }

    private void setup(final FMLCommonSetupEvent event) {
        LOGGER.info("VillageWorks setup complete");
    }

    @SubscribeEvent
    public void onEntityAttributeCreation(EntityAttributeCreationEvent event) {
        event.put(EntityTypes.VILLAGER.get(), Villager.createAttributes().build());
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        VillageWorksCommand.register(event.getDispatcher());
    }
}
