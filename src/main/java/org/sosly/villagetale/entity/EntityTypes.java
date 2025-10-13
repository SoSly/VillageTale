package org.sosly.villagetale.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.sosly.villagetale.VillageTale;

public class EntityTypes {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
        DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, VillageTale.MOD_ID);

    public static final RegistryObject<EntityType<Villager>> VILLAGER =
        ENTITY_TYPES.register("villager", () -> EntityType.Builder.of(Villager::new, MobCategory.CREATURE)
            .sized(0.6F, 1.95F)
            .clientTrackingRange(10)
            .build("villager"));

    public static final RegistryObject<EntityType<FishingBobber>> FISHING_BOBBER =
        ENTITY_TYPES.register("fishing_bobber", () -> EntityType.Builder.<FishingBobber>of(FishingBobber::new, MobCategory.MISC)
            .sized(0.25F, 0.25F)
            .clientTrackingRange(4)
            .updateInterval(5)
            .build("fishing_bobber"));

    public static void register(IEventBus eventBus) {
        ENTITY_TYPES.register(eventBus);
    }
}
