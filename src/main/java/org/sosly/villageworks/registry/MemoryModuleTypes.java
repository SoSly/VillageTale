package org.sosly.villageworks.registry;

import com.mojang.serialization.Codec;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.sosly.villageworks.VillageWorks;

import java.util.Optional;

public class MemoryModuleTypes {
    public static final DeferredRegister<MemoryModuleType<?>> MEMORY_MODULE_TYPES = 
        DeferredRegister.create(ForgeRegistries.MEMORY_MODULE_TYPES, VillageWorks.MOD_ID);

    public static final RegistryObject<MemoryModuleType<Boolean>> CAN_EAT = 
        MEMORY_MODULE_TYPES.register("can_eat", () -> new MemoryModuleType<>(Optional.of(Codec.BOOL)));

    public static final RegistryObject<MemoryModuleType<Boolean>> IS_HUNGRY = 
        MEMORY_MODULE_TYPES.register("is_hungry", () -> new MemoryModuleType<>(Optional.of(Codec.BOOL)));

    public static final RegistryObject<MemoryModuleType<Boolean>> IS_STARVING = 
        MEMORY_MODULE_TYPES.register("is_starving", () -> new MemoryModuleType<>(Optional.of(Codec.BOOL)));

    public static void register(IEventBus eventBus) {
        MEMORY_MODULE_TYPES.register(eventBus);
    }
}