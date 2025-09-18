package org.sosly.villagetale.entity.ai;

import net.minecraft.world.entity.ai.sensing.SensorType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.sosly.villagetale.VillageTale;
import org.sosly.villagetale.entity.ai.sensor.HasFoodSensor;
import org.sosly.villagetale.entity.ai.sensor.HasResourceSensor;
import org.sosly.villagetale.entity.ai.sensor.HasToolSensor;
import org.sosly.villagetale.entity.ai.sensor.HungerSensor;

public class SensorTypes {
    public static final DeferredRegister<SensorType<?>> SENSOR_TYPES =
        DeferredRegister.create(ForgeRegistries.SENSOR_TYPES, VillageTale.MOD_ID);

    public static final RegistryObject<SensorType<HungerSensor>> HUNGER =
        SENSOR_TYPES.register("hunger", () -> new SensorType<>(HungerSensor::new));

    public static final RegistryObject<SensorType<HasFoodSensor>> HAS_FOOD =
        SENSOR_TYPES.register("has_food", () -> new SensorType<>(HasFoodSensor::new));

    public static final RegistryObject<SensorType<HasToolSensor>> HAS_TOOL =
        SENSOR_TYPES.register("has_tool", () -> new SensorType<>(HasToolSensor::new));

    public static final RegistryObject<SensorType<HasResourceSensor>> HAS_RESOURCE =
        SENSOR_TYPES.register("has_resource", () -> new SensorType<>(HasResourceSensor::new));

    public static void register(IEventBus eventBus) {
        SENSOR_TYPES.register(eventBus);
    }
}
