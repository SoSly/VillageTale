package org.sosly.villagetale.profession.professions;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.entity.schedule.Schedule;
import net.minecraft.world.entity.schedule.ScheduleBuilder;
import net.minecraft.world.item.ItemStack;
import org.sosly.villagetale.VillageTale;
import org.sosly.villagetale.api.IVillageZone;
import org.sosly.villagetale.entity.Activities;
import org.sosly.villagetale.api.IWantedItem;
import org.sosly.villagetale.data.WantedItem;
import org.sosly.villagetale.entity.MemoryModuleTypes;
import org.sosly.villagetale.entity.Villager;
import org.sosly.villagetale.entity.ai.SensorTypes;
import org.sosly.villagetale.entity.ai.behavior.HarvestCropBehavior;
import org.sosly.villagetale.entity.ai.behavior.PickUpItems;
import org.sosly.villagetale.entity.ai.behavior.PlantCrops;
import org.sosly.villagetale.entity.ai.behavior.TillSoil;
import org.sosly.villagetale.profession.AbstractProfession;
import org.sosly.villagetale.zone.type.Farmland;

public class Farmer extends AbstractProfession {
    public final static ResourceLocation ID = new ResourceLocation(VillageTale.MOD_ID, "farmer");

    public Farmer() {
        super(ID);
    }

    @Override
    public Optional<IWantedItem> getAlwaysWantedItems() {
        return Optional.of(new WantedItem((ItemStack stack) -> stack
                .is(ItemTags.VILLAGER_PLANTABLE_SEEDS), 16, 0));
    }

    @Override
    public Optional<IWantedItem> getTool() {
        return Optional.of(new WantedItem((ItemStack stack) -> stack
                .is(ItemTags.HOES), 1, 1));
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
                .changeActivityAt(0, Activity.WORK)                          // WORK  (6 AM - 4 PM, 10-hour work day)
                .changeActivityAt(10000, Activities.EVENING_IDLE.get())      // EVENING_IDLE  (4 PM - 8 PM, evening social)
                .changeActivityAt(14000, Activity.REST)                      // REST  (8 PM - 4 AM, sleep)
                .changeActivityAt(22000, Activities.MORNING_IDLE.get())      // MORNING_IDLE  (4 AM - 6 AM, morning wakeup)
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
            Pair.of(1, new PickUpItems()),
            Pair.of(11, new HarvestCropBehavior()),
            Pair.of(12, new PlantCrops()),
            Pair.of(13, new TillSoil())
        );
    }

    @Override
    public boolean isValidWorkZone(IVillageZone zone) {
        return zone.getType().getID().equals(Farmland.ID);
    }
}
