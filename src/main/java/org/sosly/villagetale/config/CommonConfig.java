package org.sosly.villagetale.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import org.sosly.villagetale.VillageTale;

@Mod.EventBusSubscriber(modid = VillageTale.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class CommonConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    static {
        BUILDER.push("Village Tale Config");

        BUILDER.push("General Village Settings");
    }

    private static final ForgeConfigSpec.IntValue DEFAULT_SQUADIUS = BUILDER
            .comment("Default village squadius (radius in chunks). Village boundary = (2 * squadius + 1)^2 chunks")
            .defineInRange("defaultSquadius", 3, 1, 16);

    private static final ForgeConfigSpec.IntValue MIN_VILLAGE_DISTANCE = BUILDER
            .comment("Minimum distance between villages in chunks")
            .defineInRange("minVillageDistance", 32, 32, Integer.MAX_VALUE);

    static {
        BUILDER.pop();
        BUILDER.push("Villager Settings");
    }

    private static final ForgeConfigSpec.DoubleValue COLLECTION_DISTANCE = BUILDER
            .comment("The range at which villagers can collect items off the ground")
            .defineInRange("collectionDistance", 2.5d, 1, 16);
    private static final ForgeConfigSpec.DoubleValue INTERACTION_DISTANCE = BUILDER
            .comment("The range at which villagers can interact with blocks.")
            .defineInRange("interactionDistance", 4.5d, 2, 10);
    private static final ForgeConfigSpec.DoubleValue SCAN_RADIUS = BUILDER
            .comment("The range villagers will scan for dropped items or in storage containers")
            .defineInRange("scanRadius", 16d, 4, 32);

    private static final ForgeConfigSpec.IntValue MILK_COOLDOWN_TICKS = BUILDER
            .comment("Cooldown in ticks before a cow can be milked again (20 ticks = 1 second)")
            .defineInRange("milkCooldownTicks", 12000, 1200, 48000);

    private static final ForgeConfigSpec.IntValue PLUCK_COOLDOWN_TICKS = BUILDER
            .comment("Cooldown in ticks before a chicken can be plucked again (20 ticks = 1 second)")
            .defineInRange("pluckCooldownTicks", 12000, 1200, 48000);

    static {
        BUILDER.pop();
        BUILDER.pop();
    }

    public static ForgeConfigSpec build() {
        return BUILDER.build();
    }

    // Runtime Values
    public static double collectionDistance;
    public static int defaultSquadius;
    public static double interactionDistance;
    public static int milkCooldownTicks;
    public static int minVillageDistance;
    public static int pluckCooldownTicks;
    public static double scanRadius;

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        collectionDistance = COLLECTION_DISTANCE.get();
        defaultSquadius = DEFAULT_SQUADIUS.get();
        interactionDistance = INTERACTION_DISTANCE.get();
        milkCooldownTicks = MILK_COOLDOWN_TICKS.get();
        minVillageDistance = MIN_VILLAGE_DISTANCE.get();
        pluckCooldownTicks = PLUCK_COOLDOWN_TICKS.get();
        scanRadius = SCAN_RADIUS.get();
    }
}
