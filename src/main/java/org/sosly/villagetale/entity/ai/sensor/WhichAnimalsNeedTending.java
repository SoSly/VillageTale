package org.sosly.villagetale.entity.ai.sensor;

import com.google.common.collect.ImmutableSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.MushroomCow;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.animal.goat.Goat;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import org.jetbrains.annotations.NotNull;
import org.sosly.villagetale.VillageTale;
import org.sosly.villagetale.api.IVillageZone;
import org.sosly.villagetale.api.capability.IVillageCapability;
import org.sosly.villagetale.config.CommonConfig;
import org.sosly.villagetale.data.NBTTags;
import org.sosly.villagetale.entity.MemoryModuleTypes;
import org.sosly.villagetale.entity.Villager;
import org.sosly.villagetale.helper.VillagesHelper;
import org.sosly.villagetale.zone.type.Pen;

public class WhichAnimalsNeedTending extends Sensor<Villager> {

    public WhichAnimalsNeedTending() {
        super(200);
    }

    @Override
    protected void doTick(ServerLevel level, Villager villager) {
        if (villager.getVillage().isEmpty()) {
            return;
        }

        if (!villager.getBrain().hasMemoryValue(MemoryModuleTypes.WORK_ZONE.get())) {
            return;
        }

        IVillageCapability village = VillagesHelper.getVillageCapability(level, villager.getVillage().get());
        if (village == null) {
            return;
        }

        Optional<IVillageZone> workZone = village.getZones()
            .stream()
            .filter(z -> z.getUUID().equals(villager.getBrain().getMemory(MemoryModuleTypes.WORK_ZONE.get()).orElse(null)))
            .filter(z -> z.getType().getID().equals(Pen.ID))
            .findFirst();

        if (workZone.isEmpty()) {
            return;
        }

        IVillageZone pen = workZone.get();
        List<ItemStack> filter = pen.getFilter();

        if (filter.isEmpty()) {
            return;
        }

        List<Animal> nearbyAnimals = level.getEntitiesOfClass(Animal.class, villager.getBoundingBox().inflate(CommonConfig.scanRadius),
            animal -> animal.distanceToSqr(villager) <= CommonConfig.scanRadius * CommonConfig.scanRadius 
                && pen.containsPosition(animal.blockPosition()) 
                && matchesFilter(animal.getType(), filter));

        long currentTime = level.getGameTime();

        villager.getBrain().eraseMemory(MemoryModuleTypes.MILKABLE_ANIMAL.get());
        villager.getBrain().eraseMemory(MemoryModuleTypes.PLUCKABLE_ANIMAL.get());
        villager.getBrain().eraseMemory(MemoryModuleTypes.SHEARABLE_ANIMAL.get());
        villager.getBrain().eraseMemory(MemoryModuleTypes.BREEDABLE_ANIMAL.get());

        for (Animal animal : nearbyAnimals) {
            if (animal instanceof Chicken chicken && !chicken.isBaby()) {
                if (isPluckable(chicken, currentTime)) {
                    villager.getBrain().setMemoryWithExpiry(MemoryModuleTypes.PLUCKABLE_ANIMAL.get(), chicken, 200);
                    VillageTale.LOGGER.debug("Found pluckable chicken: " + chicken);
                }
            }

            if ((animal instanceof Cow || animal instanceof MushroomCow || animal instanceof Goat) && !animal.isBaby()) {
                if (isMilkable(animal, currentTime)) {
                    villager.getBrain().setMemoryWithExpiry(MemoryModuleTypes.MILKABLE_ANIMAL.get(), animal, 200);
                    VillageTale.LOGGER.debug("Found milkable animal: " + animal);
                }
            }

            if (animal instanceof Sheep sheep && !sheep.isBaby() && !sheep.isSheared()) {
                villager.getBrain().setMemoryWithExpiry(MemoryModuleTypes.SHEARABLE_ANIMAL.get(), sheep, 200);
                VillageTale.LOGGER.debug("Found shearable sheep: " + sheep);
            }

            if (!animal.isBaby() && animal.canFallInLove() && !animal.isInLove()) {
                villager.getBrain().setMemoryWithExpiry(MemoryModuleTypes.BREEDABLE_ANIMAL.get(), animal, 200);
                VillageTale.LOGGER.debug("Found breedable animal: " + animal);
            }
        }
    }

    private boolean isPluckable(Chicken chicken, long currentTime) {
        CompoundTag tag = chicken.getPersistentData();
        if (!tag.contains(NBTTags.PLUCK_COOLDOWN)) {
            return true;
        }
        
        long lastPluckTime = tag.getLong(NBTTags.PLUCK_COOLDOWN);
        return currentTime - lastPluckTime >= CommonConfig.pluckCooldownTicks;
    }
    
    private boolean isMilkable(Animal animal, long currentTime) {
        CompoundTag tag = animal.getPersistentData();
        if (!tag.contains(NBTTags.MILK_COOLDOWN)) {
            return true;
        }
        
        long lastMilkTime = tag.getLong(NBTTags.MILK_COOLDOWN);
        return currentTime - lastMilkTime >= CommonConfig.milkCooldownTicks;
    }

    private boolean matchesFilter(EntityType<?> entityType, List<ItemStack> filter) {
        for (ItemStack filterItem : filter) {
            if (filterItem.getItem() instanceof SpawnEggItem spawnEgg) {
                if (spawnEgg.getType(filterItem.getTag()) == entityType) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public @NotNull Set<MemoryModuleType<?>> requires() {
        return ImmutableSet.of(
            MemoryModuleTypes.WORK_ZONE.get(),
            MemoryModuleTypes.PLUCKABLE_ANIMAL.get(),
            MemoryModuleTypes.MILKABLE_ANIMAL.get(),
            MemoryModuleTypes.SHEARABLE_ANIMAL.get(),
            MemoryModuleTypes.BREEDABLE_ANIMAL.get()
        );
    }
}
