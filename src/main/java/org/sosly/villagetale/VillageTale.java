package org.sosly.villagetale;

import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import org.sosly.villagetale.block.BlockTypes;
import org.sosly.villagetale.command.VillageTaleCommand;
import org.sosly.villagetale.config.CommonConfig;
import org.sosly.villagetale.entity.Activities;
import org.sosly.villagetale.entity.EntityTypes;
import org.sosly.villagetale.entity.MemoryModuleTypes;
import org.sosly.villagetale.entity.Villager;
import org.sosly.villagetale.entity.ai.SensorTypes;
import org.sosly.villagetale.event.RegisterProfessionsEvent;
import org.sosly.villagetale.event.RegisterZoneTypesEvent;
import org.sosly.villagetale.network.NetworkHandler;
import org.sosly.villagetale.profession.ProfessionRegistry;
import org.sosly.villagetale.zone.ZoneRegistry;

@Mod(VillageTale.MOD_ID)
public class VillageTale {
    public static final String MOD_ID = "villagetale";
    public static final Logger LOGGER = LogUtils.getLogger();

    public VillageTale() {
        LOGGER.info("Loading VillageTale");

        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        BlockTypes.register(modEventBus);
        EntityTypes.register(modEventBus);
        MemoryModuleTypes.register(modEventBus);
        SensorTypes.register(modEventBus);
        Activities.register(modEventBus);

        modEventBus.addListener(this::setup);
        modEventBus.addListener(this::onEntityAttributeCreation);

        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, CommonConfig.build());

        MinecraftForge.EVENT_BUS.register(this);
    }

    private void setup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            NetworkHandler.init();

            MinecraftForge.EVENT_BUS.post(new RegisterZoneTypesEvent(ZoneRegistry.INSTANCE));
            MinecraftForge.EVENT_BUS.post(new RegisterProfessionsEvent(ProfessionRegistry.INSTANCE));

            LOGGER.info("VillageTale setup complete");
        });
    }

    @SubscribeEvent
    public void onEntityAttributeCreation(EntityAttributeCreationEvent event) {
        event.put(EntityTypes.VILLAGER.get(), Villager.createAttributes().build());
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        VillageTaleCommand.register(event.getDispatcher());
    }
}
