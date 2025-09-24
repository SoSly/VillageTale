package org.sosly.villagetale.entity;

import com.mojang.serialization.Codec;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.sosly.villagetale.VillageTale;
import org.sosly.villagetale.api.IWantedItem;
import org.sosly.villagetale.api.serialization.Codecs;
import org.sosly.villagetale.data.FoundItem;
import org.sosly.villagetale.data.TimedWantedItem;

public class MemoryModuleTypes {
    public static final DeferredRegister<MemoryModuleType<?>> MEMORY_MODULE_TYPES =
        DeferredRegister.create(ForgeRegistries.MEMORY_MODULE_TYPES, VillageTale.MOD_ID);

    // Shared Memories
    public static final RegistryObject<MemoryModuleType<List<UUID>>> ALREADY_SCANNED_STORAGES =
        MEMORY_MODULE_TYPES.register("already_scanned_storages",
                () -> new MemoryModuleType<>(Optional.empty()));

    public static final RegistryObject<MemoryModuleType<Boolean>> CAN_EAT =
        MEMORY_MODULE_TYPES.register("can_eat",
                () -> new MemoryModuleType<>(Optional.of(Codec.BOOL)));

    public static final RegistryObject<MemoryModuleType<List<TimedWantedItem>>> COULD_NOT_FIND_ITEM =
        MEMORY_MODULE_TYPES.register("could_not_find_item",
                () -> new MemoryModuleType<>(Optional.empty()));

    public static final RegistryObject<MemoryModuleType<FoundItem>> FOUND_ITEM =
            MEMORY_MODULE_TYPES.register("found_item",
                    () -> new MemoryModuleType<>(Optional.empty()));

    public static final RegistryObject<MemoryModuleType<Boolean>> IS_HUNGRY =
        MEMORY_MODULE_TYPES.register("is_hungry",
                () -> new MemoryModuleType<>(Optional.of(Codec.BOOL)));

    public static final RegistryObject<MemoryModuleType<Boolean>> IS_STARVING =
        MEMORY_MODULE_TYPES.register("is_starving",
                () -> new MemoryModuleType<>(Optional.of(Codec.BOOL)));

    public static final RegistryObject<MemoryModuleType<Long>> LAST_DAILY_EXHAUSTION =
        MEMORY_MODULE_TYPES.register("last_daily_exhaustion",
                () -> new MemoryModuleType<>(Optional.of(Codec.LONG)));

    public static final RegistryObject<MemoryModuleType<Map<ResourceLocation, Integer>>> ITEMS_TO_DEPOSIT =
            MEMORY_MODULE_TYPES.register("items_to_deposit",
                    () -> new MemoryModuleType<>(Optional.empty()));
    public static final RegistryObject<MemoryModuleType<ResourceLocation>> PROFESSION =
        MEMORY_MODULE_TYPES.register("profession",
                () -> new MemoryModuleType<>(Optional.of(ResourceLocation.CODEC)));

    public static final RegistryObject<MemoryModuleType<UUID>> VILLAGE =
        MEMORY_MODULE_TYPES.register("village",
                () -> new MemoryModuleType<>(Optional.of(Codecs.UUID)));

    public static final RegistryObject<MemoryModuleType<IWantedItem>> WANTED_ITEM =
        MEMORY_MODULE_TYPES.register("wanted_item",
                () -> new MemoryModuleType<>(Optional.empty()));

    public static final RegistryObject<MemoryModuleType<UUID>> WORK_ZONE =
            MEMORY_MODULE_TYPES.register("work_zone",
                    () -> new MemoryModuleType<>(Optional.of(Codecs.UUID)));

    public static final RegistryObject<MemoryModuleType<GlobalPos>> WORK_POS =
            MEMORY_MODULE_TYPES.register("work_pos",
                    () -> new MemoryModuleType<>(Optional.of(GlobalPos.CODEC)));

    public static final RegistryObject<MemoryModuleType<UUID>> HOME_ZONE =
            MEMORY_MODULE_TYPES.register("home_zone",
                    () -> new MemoryModuleType<>(Optional.of(Codecs.UUID)));

    public static final RegistryObject<MemoryModuleType<Boolean>> BUSY =
            MEMORY_MODULE_TYPES.register("busy",
                    () -> new MemoryModuleType<>(Optional.empty()));

    // Crafter Memories
    public static final RegistryObject<MemoryModuleType<ResourceLocation>> CURRENT_RECIPE =
            MEMORY_MODULE_TYPES.register("current_recipe",
                    () -> new MemoryModuleType<>(Optional.empty()));
    public static final RegistryObject<MemoryModuleType<BlockPos>> NEAREST_WORKSTATION =
            MemoryModuleTypes.MEMORY_MODULE_TYPES.register("nearest_workstation",
                    () -> new MemoryModuleType<>(Optional.empty()));

    // Farmer Memories
    public static final RegistryObject<MemoryModuleType<BlockPos>> NEAREST_TILLABLE_SOIL =
            MemoryModuleTypes.MEMORY_MODULE_TYPES.register("nearest_tillable_soil",
                    () -> new MemoryModuleType<>(Optional.of(BlockPos.CODEC)));
    public static final RegistryObject<MemoryModuleType<BlockPos>> NEAREST_EMPTY_FARMLAND =
            MemoryModuleTypes.MEMORY_MODULE_TYPES.register("nearest_empty_farmland",
                    () -> new MemoryModuleType<>(Optional.of(BlockPos.CODEC)));
    public static final RegistryObject<MemoryModuleType<BlockPos>> NEAREST_HARVESTABLE_CROP =
            MemoryModuleTypes.MEMORY_MODULE_TYPES.register("nearest_harvestable_crop",
                    () -> new MemoryModuleType<>(Optional.of(BlockPos.CODEC)));

    // Lumberjack Memories
    public static final RegistryObject<MemoryModuleType<BlockPos>> NEAREST_LOG =
            MemoryModuleTypes.MEMORY_MODULE_TYPES.register("nearest_log",
                    () -> new MemoryModuleType<>(Optional.of(BlockPos.CODEC)));
    public static final RegistryObject<MemoryModuleType<BlockPos>> NEAREST_REPLANTABLE_SPOT =
            MemoryModuleTypes.MEMORY_MODULE_TYPES.register("nearest_replantable_spot",
                    () -> new MemoryModuleType<>(Optional.of(BlockPos.CODEC)));


    public static void register(IEventBus eventBus) {
        MEMORY_MODULE_TYPES.register(eventBus);
    }
}
