package org.sosly.villagetale.entity;

import net.minecraft.world.entity.schedule.Activity;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.sosly.villagetale.VillageTale;

public class Activities {
    public static final DeferredRegister<Activity> ACTIVITIES = 
        DeferredRegister.create(ForgeRegistries.ACTIVITIES, VillageTale.MOD_ID);
    
    public static final RegistryObject<Activity> MORNING_IDLE = 
        ACTIVITIES.register("morning_idle", () -> new Activity("morning_idle"));
    
    public static final RegistryObject<Activity> EVENING_IDLE = 
        ACTIVITIES.register("evening_idle", () -> new Activity("evening_idle"));
    
    public static void register(IEventBus eventBus) {
        ACTIVITIES.register(eventBus);
    }
}