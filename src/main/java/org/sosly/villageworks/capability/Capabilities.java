package org.sosly.villageworks.capability;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.sosly.villageworks.VillageWorks;
import org.sosly.villageworks.api.capability.IVillageCapability;
import org.sosly.villageworks.capability.village.VillageCapability;
import org.sosly.villageworks.capability.village.VillageProvider;

import java.util.UUID;

@Mod.EventBusSubscriber(modid = VillageWorks.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class Capabilities {

    public static final Capability<IVillageCapability> VILLAGE_CAPABILITY =
        CapabilityManager.get(new CapabilityToken<>() {});

    private static final ResourceLocation VILLAGE_CAPABILITY_KEY =
        new ResourceLocation(VillageWorks.MOD_ID, "village_capability");

    public static void register() {
    }

    @SubscribeEvent
    public static void onAttachCapabilities(AttachCapabilitiesEvent<LevelChunk> event) {
        LevelChunk chunk = event.getObject();

        if (!containsTownHall(chunk)) {
            return;
        }

        UUID villageId = UUID.randomUUID();
        VillageProvider provider = new VillageProvider(villageId, chunk.getPos());

        provider.getCapability(VILLAGE_CAPABILITY, null)
            .ifPresent(cap -> {
                if (cap instanceof VillageCapability impl) {
                    impl.setOwnerChunk(chunk);
                }
            });

        event.addCapability(VILLAGE_CAPABILITY_KEY, provider);
    }

    private static boolean containsTownHall(LevelChunk chunk) {
        // TODO: Implement Town Hall detection logic
        // This should scan the chunk for Town Hall blocks
        return false;
    }
}
