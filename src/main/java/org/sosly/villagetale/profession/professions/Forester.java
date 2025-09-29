package org.sosly.villagetale.profession.professions;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.ai.sensing.SensorType;
import org.sosly.villagetale.VillageTale;
import org.sosly.villagetale.entity.MemoryModuleTypes;
import org.sosly.villagetale.entity.Villager;
import org.sosly.villagetale.entity.ai.SensorTypes;
import org.sosly.villagetale.entity.ai.behavior.ChopTree;
import org.sosly.villagetale.entity.ai.behavior.PickUpItems;
import org.sosly.villagetale.entity.ai.behavior.PlantSapling;

public class Forester extends AbstractProfession {
    public final static ResourceLocation ID = new ResourceLocation(VillageTale.MOD_ID, "forester");

    public Forester() {
        super(ID);
    }


    @Override
    public ImmutableList<MemoryModuleType<?>> getMemoryModules() {
        return ImmutableList.of(
            MemoryModuleTypes.NEAREST_TREE.get(),
            MemoryModuleTypes.NEAREST_REPLANTABLE_SPOT.get()
        );
    }

    @Override
    public ImmutableList<SensorType<? extends Sensor<? super Villager>>> getSensors() {
        return ImmutableList.of(
            SensorTypes.IS_FOREST.get(),
            SensorTypes.HAS_WORK_ZONE.get()
        );
    }

    @Override
    public ImmutableList<? extends Pair<Integer, ? extends BehaviorControl<? super Villager>>> getWorkPackage(float speedModifier) {
        return ImmutableList.of(
            Pair.of(1, new PickUpItems()),
            Pair.of(10, new ChopTree()),
            Pair.of(11, new PlantSapling())
        );
    }
}
