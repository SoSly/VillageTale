package org.sosly.villageworks.block.entity;

import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.sosly.villageworks.VillageWorks;
import org.sosly.villageworks.block.BlockTypes;

public class BlockEntityTypes {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
        DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, VillageWorks.MOD_ID);

    public static final RegistryObject<BlockEntityType<TownHallBlockEntity>> TOWNHALL =
        BLOCK_ENTITIES.register("townhall", () -> BlockEntityType.Builder.of(
            TownHallBlockEntity::new, BlockTypes.TOWNHALL.get()).build(null));

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}