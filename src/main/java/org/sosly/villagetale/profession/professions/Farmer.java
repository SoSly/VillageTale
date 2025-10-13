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
import org.sosly.villagetale.entity.ai.behavior.HarvestCrop;
import org.sosly.villagetale.entity.ai.behavior.PlantCrops;
import org.sosly.villagetale.entity.ai.behavior.TillSoil;

public class Farmer extends AbstractProfession {
    public static final ResourceLocation ID = new ResourceLocation(VillageTale.MOD_ID, "farmer");

    public Farmer() {
        super(ID);
    }


    @Override
    public ImmutableList<MemoryModuleType<?>> getMemoryModules() {
        return ImmutableList.of(
            MemoryModuleTypes.NEAREST_TILLABLE_SOIL.get(),
            MemoryModuleTypes.NEAREST_EMPTY_FARMLAND.get(),
            MemoryModuleTypes.NEAREST_HARVESTABLE_CROP.get()
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
            SensorTypes.IS_FARMLAND.get(),
            SensorTypes.HAS_WORK_ZONE.get()
        );
    }

    @Override
    public ImmutableList<? extends Pair<Integer, ? extends BehaviorControl<? super Villager>>> getWorkPackage(float speedModifier) {
        return ImmutableList.of(
            Pair.of(10, new TillSoil()),
            Pair.of(11, new HarvestCrop()),
            Pair.of(12, new PlantCrops())
        );
    }
}
