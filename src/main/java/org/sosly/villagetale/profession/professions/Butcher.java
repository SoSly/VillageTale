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
import org.sosly.villagetale.entity.ai.behavior.BringAnimalToButchery;
import org.sosly.villagetale.entity.ai.behavior.GoToNearestPen;
import org.sosly.villagetale.entity.ai.behavior.SlaughterAnimal;

public class Butcher extends AbstractProfession {
    public final static ResourceLocation ID = new ResourceLocation(VillageTale.MOD_ID, "butcher");

    public Butcher() {
        super(ID);
    }

    @Override
    public ImmutableList<MemoryModuleType<?>> getMemoryModules() {
        return ImmutableList.of(
            MemoryModuleTypes.ALREADY_SCANNED_PENS.get(),
            MemoryModuleTypes.FOUND_ENTITY.get(),
            MemoryModuleTypes.SLAUGHTERABLE_ENTITY.get()
        );
    }

    @Override
    public Schedule getSchedule() {
        return new ScheduleBuilder(new Schedule())
                .changeActivityAt(0, Activity.WORK)
                .changeActivityAt(10000, Activities.EVENING_IDLE.get())
                .changeActivityAt(14000, Activity.REST)
                .changeActivityAt(22000, Activities.MORNING_IDLE.get())
                .build();
    }

    @Override
    public ImmutableList<SensorType<? extends Sensor<? super Villager>>> getSensors() {
        return ImmutableList.of(
            SensorTypes.HAS_WORK_ZONE.get(),
            SensorTypes.IS_ENTITY_IN_PEN.get(),
            SensorTypes.WHICH_ANIMALS_NEED_SLAUGHTERING.get()
        );
    }

    @Override
    public ImmutableList<? extends Pair<Integer, ? extends BehaviorControl<? super Villager>>> getWorkPackage(float speedModifier) {
        return ImmutableList.of(
                Pair.of(8, new SlaughterAnimal()),
                Pair.of(9, new BringAnimalToButchery()),
                Pair.of(10, new GoToNearestPen())
        );
    }
}
