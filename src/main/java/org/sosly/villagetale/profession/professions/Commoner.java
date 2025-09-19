package org.sosly.villagetale.profession.professions;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.ai.sensing.SensorType;
import org.sosly.villagetale.VillageTale;
import org.sosly.villagetale.api.IVillageZone;
import org.sosly.villagetale.api.IWantedItem;
import org.sosly.villagetale.entity.Villager;
import org.sosly.villagetale.profession.AbstractProfession;

public class Commoner extends AbstractProfession {
    public final static ResourceLocation ID = new ResourceLocation(VillageTale.MOD_ID, "commoner");
    public Commoner() {
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

    @Override
    public boolean isValidWorkZone(IVillageZone zone) {
        return false;
    }
}
