package org.sosly.villagetale.capability;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.sosly.villagetale.VillageTale;
import org.sosly.villagetale.api.capability.IVillageCapability;
import org.sosly.villagetale.api.capability.IVillagesCapability;
import org.sosly.villagetale.capability.village.VillageProvider;
import org.sosly.villagetale.capability.villages.VillagesProvider;

@Mod.EventBusSubscriber(modid = VillageTale.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class Capabilities {

    public static final Capability<IVillageCapability> VILLAGE_CAPABILITY =
        CapabilityManager.get(new CapabilityToken<>() {});

    public static final Capability<IVillagesCapability> VILLAGES_CAPABILITY =
        CapabilityManager.get(new CapabilityToken<>() {});

    private static final ResourceLocation VILLAGE_CAPABILITY_KEY =
        new ResourceLocation(VillageTale.MOD_ID, "village_capability");

    private static final ResourceLocation VILLAGES_CAPABILITY_KEY =
        new ResourceLocation(VillageTale.MOD_ID, "villages_capability");

    @SubscribeEvent
    public static void onAttachCapabilities(AttachCapabilitiesEvent<LevelChunk> event) {
        LevelChunk chunk = event.getObject();

        VillageProvider provider = new VillageProvider();
        IVillageCapability village = provider.getCapability(VILLAGE_CAPABILITY, null).resolve().get();
        village.setChunk(chunk);
        event.addCapability(VILLAGE_CAPABILITY_KEY, provider);
    }

    @SubscribeEvent
    public static void onAttachLevelCapabilities(AttachCapabilitiesEvent<Level> event) {
        VillagesProvider provider = new VillagesProvider();
        event.addCapability(VILLAGES_CAPABILITY_KEY, provider);
    }
}
