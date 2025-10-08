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
    
    private static final ForgeConfigSpec.IntValue MILK_COOLDOWN_TICKS = builder
            .comment("Cooldown in ticks before a cow can be milked again (20 ticks = 1 second)")
            .defineInRange("milkCooldownTicks", 12000, 1200, 48000);
    
    private static final ForgeConfigSpec.IntValue PLUCK_COOLDOWN_TICKS = builder
            .comment("Cooldown in ticks before a chicken can be plucked again (20 ticks = 1 second)")
            .defineInRange("pluckCooldownTicks", 12000, 1200, 48000);

    private static final ForgeConfigSpec.IntValue DEFAULT_SQUADIUS = builder
            .comment("Default village squadius (radius in chunks). Village boundary = (2 * squadius + 1)^2 chunks")
            .defineInRange("defaultSquadius", 3, 1, 16);

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
    public static int milkCooldownTicks;
    public static int pluckCooldownTicks;
    public static int defaultSquadius;

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        collectionDistance = COLLECTION_DISTANCE.get();
        interactionDistance = INTERACTION_DISTANCE.get();
        scanRadius = SCAN_RADIUS.get();
        milkCooldownTicks = MILK_COOLDOWN_TICKS.get();
        pluckCooldownTicks = PLUCK_COOLDOWN_TICKS.get();
        defaultSquadius = DEFAULT_SQUADIUS.get();
    }
}
