package org.sosly.villagetale.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import org.sosly.villagetale.VillageTale;

@Mod.EventBusSubscriber(modid = VillageTale.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class CommonConfig {
    private static final ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

    static {
        builder.push("Village Tale Config");
        builder.comment("General settings for all villagers");
    }

    private static final ForgeConfigSpec.DoubleValue COLLECTION_DISTANCE = builder
            .comment("The range at which villagers can collect items off the ground")
            .defineInRange("collectionDistance", 2.5d, 1, 16);
    private static final ForgeConfigSpec.DoubleValue INTERACTION_DISTANCE = builder
            .comment("The range at which villagers can interact with blocks.")
            .defineInRange("interactionDistance", 4.5d, 2, 10);
    private static final ForgeConfigSpec.DoubleValue SCAN_RADIUS = builder
            .comment("The range villagers will scan for dropped items or in storage containers")
            .defineInRange("scanRadius", 16d, 4, 32);

    static {
        builder.pop();
    }

    public static ForgeConfigSpec build() {
        return builder.build();
    }

    // Runtime Values
    public static double collectionDistance;
    public static double interactionDistance;
    public static double scanRadius;

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        collectionDistance = COLLECTION_DISTANCE.get();
        interactionDistance = INTERACTION_DISTANCE.get();
        scanRadius = SCAN_RADIUS.get();
    }
}
