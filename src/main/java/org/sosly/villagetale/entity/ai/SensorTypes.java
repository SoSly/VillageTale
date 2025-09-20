package org.sosly.villagetale.entity.ai;

import net.minecraft.world.entity.ai.sensing.SensorType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.sosly.villagetale.VillageTale;
import org.sosly.villagetale.entity.ai.sensor.HasBedInHomeZone;
import org.sosly.villagetale.entity.ai.sensor.HasFood;
import org.sosly.villagetale.entity.ai.sensor.HasItemsToDeposit;
import org.sosly.villagetale.entity.ai.sensor.HasResources;
import org.sosly.villagetale.entity.ai.sensor.HasTool;
import org.sosly.villagetale.entity.ai.sensor.HasWorkZone;
import org.sosly.villagetale.entity.ai.sensor.IsFarmland;
import org.sosly.villagetale.entity.ai.sensor.IsHungry;
import org.sosly.villagetale.entity.ai.sensor.IsItemInStorage;

public class SensorTypes {
    public static final DeferredRegister<SensorType<?>> SENSOR_TYPES =
        DeferredRegister.create(ForgeRegistries.SENSOR_TYPES, VillageTale.MOD_ID);

    public static final RegistryObject<SensorType<HasFood>> HAS_FOOD =
        SENSOR_TYPES.register("has_food", () -> new SensorType<>(HasFood::new));

    public static final RegistryObject<SensorType<HasItemsToDeposit>> HAS_ITEMS_TO_DEPOSIT =
            SENSOR_TYPES.register("has_items_to_deposit", () -> new SensorType<>(HasItemsToDeposit::new));

    public static final RegistryObject<SensorType<HasResources>> HAS_RESOURCE =
        SENSOR_TYPES.register("has_resource", () -> new SensorType<>(HasResources::new));

    public static final RegistryObject<SensorType<HasTool>> HAS_TOOL =
            SENSOR_TYPES.register("has_tool", () -> new SensorType<>(HasTool::new));

    public static final RegistryObject<SensorType<HasWorkZone>> HAS_WORK_ZONE =
            SENSOR_TYPES.register("has_work_zone", () -> new SensorType<>(HasWorkZone::new));

    public static final RegistryObject<SensorType<IsFarmland>> IS_FARMLAND =
            SENSOR_TYPES.register("is_farmland", () -> new SensorType<>(IsFarmland::new));

    public static final RegistryObject<SensorType<IsHungry>> IS_HUNGRY =
            SENSOR_TYPES.register("is_hungry", () -> new SensorType<>(IsHungry::new));

    public static final RegistryObject<SensorType<IsItemInStorage>> IS_ITEM_IN_STORAGE =
            SENSOR_TYPES.register("is_item_in_storage", () -> new SensorType<>(IsItemInStorage::new));
    
    public static final RegistryObject<SensorType<HasBedInHomeZone>> HAS_BED_IN_HOME_ZONE =
            SENSOR_TYPES.register("has_bed_in_home_zone", () -> new SensorType<>(HasBedInHomeZone::new));


    public static void register(IEventBus eventBus) {
        SENSOR_TYPES.register(eventBus);
    }
}
