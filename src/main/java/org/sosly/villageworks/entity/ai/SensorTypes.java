package org.sosly.villageworks.entity.ai;

import net.minecraft.world.entity.ai.sensing.SensorType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.sosly.villageworks.VillageWorks;
import org.sosly.villageworks.entity.ai.sensor.HungerSensor;

public class SensorTypes {
    public static final DeferredRegister<SensorType<?>> SENSOR_TYPES =
        DeferredRegister.create(ForgeRegistries.SENSOR_TYPES, VillageWorks.MOD_ID);

    public static final RegistryObject<SensorType<HungerSensor>> HUNGER =
        SENSOR_TYPES.register("hunger", () -> new SensorType<>(HungerSensor::new));

    public static void register(IEventBus eventBus) {
        SENSOR_TYPES.register(eventBus);
    }
}
