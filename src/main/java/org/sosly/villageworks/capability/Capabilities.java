package org.sosly.villageworks.capability;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.sosly.villageworks.VillageWorks;
import org.sosly.villageworks.api.capability.IVillageCapability;
import org.sosly.villageworks.api.capability.IVillagesCapability;
import org.sosly.villageworks.block.BlockTypes;
import org.sosly.villageworks.capability.villages.VillagesCapability;
import org.sosly.villageworks.capability.villages.VillagesProvider;
import org.sosly.villageworks.capability.village.VillageCapability;
import org.sosly.villageworks.capability.village.VillageProvider;

import java.util.UUID;

@Mod.EventBusSubscriber(modid = VillageWorks.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class Capabilities {

    public static final Capability<IVillageCapability> VILLAGE_CAPABILITY =
        CapabilityManager.get(new CapabilityToken<>() {});

    public static final Capability<IVillagesCapability> VILLAGES_CAPABILITY =
        CapabilityManager.get(new CapabilityToken<>() {});

    private static final ResourceLocation VILLAGE_CAPABILITY_KEY =
        new ResourceLocation(VillageWorks.MOD_ID, "village_capability");

    private static final ResourceLocation VILLAGES_CAPABILITY_KEY =
        new ResourceLocation(VillageWorks.MOD_ID, "villages_capability");

    @SubscribeEvent
    public static void onAttachCapabilities(AttachCapabilitiesEvent<LevelChunk> event) {
        LevelChunk chunk = event.getObject();

        var villagesCapability = chunk.getLevel().getCapability(VILLAGES_CAPABILITY).orElse(null);
        if (villagesCapability == null) {
            return;
        }
        
        var villageData = villagesCapability.getVillageAt(chunk.getPos());
        if (villageData == null || !chunk.getPos().equals(villageData.getVillageStartingChunk())) {
            return;
        }

        UUID villageId = villageData.getVillageId();
        BlockPos townHallPos = villageData.getTownHallPos();
        VillageProvider provider = new VillageProvider(villageId, townHallPos, chunk.getPos());

        provider.getCapability(VILLAGE_CAPABILITY, null)
            .ifPresent(cap -> {
                if (cap instanceof VillageCapability impl) {
                    impl.setOwnerChunk(chunk);
                }
            });

        event.addCapability(VILLAGE_CAPABILITY_KEY, provider);
    }

    @SubscribeEvent
    public static void onAttachLevelCapabilities(AttachCapabilitiesEvent<Level> event) {
        Level level = event.getObject();

        VillagesProvider provider = new VillagesProvider();

        provider.getCapability(VILLAGES_CAPABILITY, null)
            .ifPresent(cap -> {
                if (cap instanceof VillagesCapability impl) {
                    impl.setOwnerLevel(level);
                }
            });

        event.addCapability(VILLAGES_CAPABILITY_KEY, provider);
    }
}
