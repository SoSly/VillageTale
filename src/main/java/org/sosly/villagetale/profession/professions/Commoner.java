package org.sosly.villagetale.profession.professions;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.ai.sensing.SensorType;
import org.sosly.villagetale.VillageTale;
import org.sosly.villagetale.entity.Villager;

public class Commoner extends AbstractProfession {
    public static final ResourceLocation ID = new ResourceLocation(VillageTale.MOD_ID, "commoner");
    public Commoner() {
        super(ID);
    }

    @Override
    public ImmutableList<MemoryModuleType<?>> getMemoryModules() {
        return ImmutableList.of();
    }

    @Override
    public ImmutableList<SensorType<? extends Sensor<? super Villager>>> getSensors() {
        return ImmutableList.of();
    }

    @Override
    public ImmutableList<? extends Pair<Integer, ? extends BehaviorControl<? super Villager>>> getWorkPackage(float speedModifier) {
        return ImmutableList.of();
    }
}
