package org.sosly.villagetale.profession.professions;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.RegistryObject;
import org.sosly.villagetale.VillageTale;
import org.sosly.villagetale.api.IVillageZone;
import org.sosly.villagetale.api.IWantedItem;
import org.sosly.villagetale.data.WantedItem;
import org.sosly.villagetale.entity.MemoryModuleTypes;
import org.sosly.villagetale.entity.Villager;
import org.sosly.villagetale.profession.AbstractProfession;
import org.sosly.villagetale.zone.type.Farmland;

public class Farmer extends AbstractProfession {
    public final static ResourceLocation ID = new ResourceLocation(VillageTale.MOD_ID, "farmer");
    public static final RegistryObject<MemoryModuleType<BlockPos>> NEAREST_TILLABLE_SOIL =
            MemoryModuleTypes.MEMORY_MODULE_TYPES.register("nearest_tillable_soil",
                    () -> new MemoryModuleType<>(Optional.of(BlockPos.CODEC)));
    public static final RegistryObject<MemoryModuleType<BlockPos>> NEAREST_EMPTY_FARMLAND =
            MemoryModuleTypes.MEMORY_MODULE_TYPES.register("nearest_empty_farmland",
                    () -> new MemoryModuleType<>(Optional.of(BlockPos.CODEC)));
    public static final RegistryObject<MemoryModuleType<BlockPos>> NEAREST_HARVESTABLE_CROP =
            MemoryModuleTypes.MEMORY_MODULE_TYPES.register("nearest_harvestable_crop",
                    () -> new MemoryModuleType<>(Optional.of(BlockPos.CODEC)));

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
            Farmer.NEAREST_TILLABLE_SOIL.get(),
            Farmer.NEAREST_EMPTY_FARMLAND.get(),
            Farmer.NEAREST_HARVESTABLE_CROP.get(),
            MemoryModuleTypes.WORK_ZONE.get()
        );
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
    public void registerAdditionalGoals(Brain<Villager> brain) {
    }

    @Override
    public boolean isValidWorkZone(IVillageZone zone) {
        return zone.getType().getID().equals(Farmland.ID);
    }
}
