package org.sosly.villagetale.profession.professions;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.entity.schedule.Schedule;
import net.minecraft.world.entity.schedule.ScheduleBuilder;
import net.minecraft.world.item.Items;
import org.sosly.villagetale.VillageTale;
import org.sosly.villagetale.api.IVillageZone;
import org.sosly.villagetale.api.IWantedItem;
import org.sosly.villagetale.data.ItemOrTagMatcher;
import org.sosly.villagetale.data.WantedItem;
import org.sosly.villagetale.data.loaders.EntityDataLoader;
import org.sosly.villagetale.entity.Activities;
import org.sosly.villagetale.entity.MemoryModuleTypes;
import org.sosly.villagetale.entity.Villager;
import org.sosly.villagetale.entity.ai.SensorTypes;
import org.sosly.villagetale.entity.ai.behavior.BringAnimalsToPen;
import org.sosly.villagetale.entity.ai.behavior.FeedAnimal;
import org.sosly.villagetale.entity.ai.behavior.MilkAnimal;
import org.sosly.villagetale.entity.ai.behavior.PluckAnimal;
import org.sosly.villagetale.entity.ai.behavior.ShearAnimal;
import org.sosly.villagetale.helper.VillagesHelper;
import org.sosly.villagetale.zone.type.Pen;

public class Herder extends AbstractProfession {
    public final static ResourceLocation ID = new ResourceLocation(VillageTale.MOD_ID, "herder");


    public Herder() {
        super(ID);
    }

    @Override
    public List<IWantedItem> getAlwaysWantedItems(Villager villager) {
        if (!(villager.level() instanceof ServerLevel serverLevel)) {
            return List.of();
        }

        IVillageZone workplace = VillagesHelper.getWorkplaceZone(serverLevel, villager);
        if (workplace == null || workplace.getEntityTypeFilter().isEmpty()) {
            return List.of(new WantedItem(stack -> stack.is(Items.LEAD), 2, 1));
        }

        List<ItemOrTagMatcher> foodMatchers = new ArrayList<>();
        List<ItemOrTagMatcher> toolMatchers = new ArrayList<>();

        Set<ResourceLocation> entityTypeFilter = workplace.getEntityTypeFilter();
        for (ResourceLocation entityId : entityTypeFilter) {
            EntityDataLoader.EntityData data = EntityDataLoader.getEntityData(entityId);
            if (!data.food.isEmpty()) {
                foodMatchers.add(data.food);
            }
            if (!data.tools.isEmpty()) {
                toolMatchers.add(data.tools);
            }
        }

        List<IWantedItem> result = new ArrayList<>();
        result.add(new WantedItem(stack -> stack.is(Items.LEAD), 1, 0));

        for (ItemOrTagMatcher matcher : foodMatchers) {
            result.add(new WantedItem(matcher::matches, 16, 0));
        }

        for (ItemOrTagMatcher matcher : toolMatchers) {
            result.add(new WantedItem(matcher::matches, 1, 0));
        }

        return result;
    }



    @Override
    public Optional<IWantedItem> getTool() {
        return Optional.of(
            new WantedItem((stack) -> stack.is(Items.LEAD), 1, 1)
        );
    }

    @Override
    public boolean isValidWorkZone(IVillageZone zone) {
        return zone.getType().getID().equals(Pen.ID);
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
    public ImmutableList<MemoryModuleType<?>> getMemoryModules() {
        return ImmutableList.of(
            MemoryModuleTypes.BREEDABLE_ANIMAL.get(),
            MemoryModuleTypes.MILKABLE_ANIMAL.get(),
            MemoryModuleTypes.PLUCKABLE_ANIMAL.get(),
            MemoryModuleTypes.SHEARABLE_ANIMAL.get(),
            MemoryModuleTypes.WANDERING_ANIMAL.get()
        );
    }

    @Override
    public ImmutableList<SensorType<? extends Sensor<? super Villager>>> getSensors() {
        return ImmutableList.of(
            SensorTypes.HAS_WORK_ZONE.get(),
            SensorTypes.IS_WANDERING_ANIMAL.get(),
            SensorTypes.WHICH_ANIMALS_NEED_TENDING.get()
        );
    }

    @Override
    public ImmutableList<? extends Pair<Integer, ? extends BehaviorControl<? super Villager>>> getWorkPackage(float speedModifier) {
        return ImmutableList.of(
                Pair.of(9, new BringAnimalsToPen()),
                Pair.of(10, new MilkAnimal()),
                Pair.of(10, new PluckAnimal()),
                Pair.of(10, new ShearAnimal()),
                Pair.of(13, new FeedAnimal())
        );
    }
}
