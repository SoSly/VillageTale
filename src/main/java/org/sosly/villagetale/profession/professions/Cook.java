package org.sosly.villagetale.profession.professions;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.entity.schedule.Schedule;
import net.minecraft.world.entity.schedule.ScheduleBuilder;
import org.sosly.villagetale.VillageTale;
import org.sosly.villagetale.api.IVillageZone;
import org.sosly.villagetale.api.IWantedItem;
import org.sosly.villagetale.entity.Activities;
import org.sosly.villagetale.entity.MemoryModuleTypes;
import org.sosly.villagetale.entity.Villager;
import org.sosly.villagetale.entity.ai.SensorTypes;
import org.sosly.villagetale.entity.ai.behavior.CraftRecipeItem;
import org.sosly.villagetale.profession.AbstractProfession;
import org.sosly.villagetale.zone.type.Kitchen;

public class Cook extends AbstractProfession {
    public final static ResourceLocation ID = new ResourceLocation(VillageTale.MOD_ID, "cook");
    private final static ResourceLocation OVERLAY_TEXTURE = new ResourceLocation(VillageTale.MOD_ID, "textures/entity/villager/profession/cook.png");

    public Cook() {
        super(ID);
    }

    @Override
    public Optional<IWantedItem> getAlwaysWantedItems() {
        return Optional.empty();
    }

    @Override
    public Optional<IWantedItem> getTool() {
        return Optional.empty();
    }

    @Override
    public ImmutableList<MemoryModuleType<?>> getMemoryModules() {
        return ImmutableList.of(
            MemoryModuleTypes.CURRENT_RECIPE.get(),
            MemoryModuleTypes.NEAREST_WORKSTATION.get()
        );
    }

    @Override
    public Schedule getSchedule() {
        return new ScheduleBuilder(new Schedule())
                .changeActivityAt(0, Activity.WORK)                          // WORK  (6 AM - 4 PM, 10-hour work day)
                .changeActivityAt(10000, Activities.EVENING_IDLE.get())      // EVENING_IDLE  (4 PM - 8 PM, evening social)
                .changeActivityAt(14000, Activity.REST)                      // REST  (8 PM - 4 AM, sleep)
                .changeActivityAt(22000, Activities.MORNING_IDLE.get())      // MORNING_IDLE  (4 AM - 6 AM, morning wakeup)
                .build();
    }

    @Override
    public ImmutableList<SensorType<? extends Sensor<? super Villager>>> getSensors() {
        return ImmutableList.of(
            SensorTypes.HAS_WORK_ZONE.get(),
            SensorTypes.WHAT_SHOULD_BE_CRAFTED.get(),
            SensorTypes.WHERE_SHOULD_I_CRAFT.get()
        );
    }

    @Override
    public ImmutableList<? extends Pair<Integer, ? extends BehaviorControl<? super Villager>>> getWorkPackage(float speedModifier) {
        return ImmutableList.of(
            Pair.of(10, new CraftRecipeItem())
        );
    }

    @Override
    public boolean isValidWorkZone(IVillageZone zone) {
        return zone.getType().getID().equals(Kitchen.ID);
    }

    @Override
    public Optional<ResourceLocation> getOverlayTexture() {
        return Optional.of(OVERLAY_TEXTURE);
    }
}
