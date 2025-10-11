package org.sosly.villagetale.profession.professions;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.entity.schedule.Schedule;
import net.minecraft.world.entity.schedule.ScheduleBuilder;
import org.sosly.villagetale.VillageTale;
import org.sosly.villagetale.entity.Activities;
import org.sosly.villagetale.entity.MemoryModuleTypes;
import org.sosly.villagetale.entity.Villager;
import org.sosly.villagetale.entity.ai.SensorTypes;
import org.sosly.villagetale.entity.ai.behavior.CraftRecipeItem;
import org.sosly.villagetale.entity.ai.behavior.TakeFromWorkstation;

public class Cook extends AbstractProfession {
    public final static ResourceLocation ID = new ResourceLocation(VillageTale.MOD_ID, "cook");

    public Cook() {
        super(ID);
    }

    @Override
    public ImmutableList<MemoryModuleType<?>> getMemoryModules() {
        return ImmutableList.of(
            MemoryModuleTypes.CURRENT_RECIPE.get(),
            MemoryModuleTypes.NEAREST_WORKSTATION.get(),
            MemoryModuleTypes.WORKSTATION_OUTPUT_READY.get(),
            MemoryModuleTypes.WORKSTATION_NEEDS_FUEL.get()
        );
    }

    @Override
    public Schedule getSchedule() {
        return new ScheduleBuilder(new Schedule())
                .changeActivityAt(0, Activity.WORK)                          // 6 AM - 4 PM
                .changeActivityAt(10000, Activities.EVENING_IDLE.get())      // 4 PM - 8 PM
                .changeActivityAt(14000, Activity.REST)                      // 8 PM - 4 AM
                .changeActivityAt(22000, Activities.MORNING_IDLE.get())      // 4 AM - 6 AM
                .build();
    }

    @Override
    public ImmutableList<SensorType<? extends Sensor<? super Villager>>> getSensors() {
        return ImmutableList.of(
            SensorTypes.HAS_WORK_ZONE.get(),
            SensorTypes.WHAT_SHOULD_BE_CRAFTED.get(),
            SensorTypes.WHERE_SHOULD_I_CRAFT.get(),
            SensorTypes.HAS_WORKSTATION_OUTPUT.get(),
            SensorTypes.DOES_WORKSTATION_NEED_FUEL.get()
        );
    }

    @Override
    public ImmutableList<? extends Pair<Integer, ? extends BehaviorControl<? super Villager>>> getWorkPackage(float speedModifier) {
        return ImmutableList.of(
                Pair.of(9, new TakeFromWorkstation()),
                Pair.of(10, new CraftRecipeItem())
        );
    }
}
