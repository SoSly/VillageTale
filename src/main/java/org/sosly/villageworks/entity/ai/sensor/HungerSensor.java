package org.sosly.villageworks.entity.ai.sensor;

import com.google.common.collect.ImmutableSet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.sensing.Sensor;
import org.sosly.villageworks.VillageWorks;
import org.sosly.villageworks.entity.Villager;
import org.sosly.villageworks.entity.MemoryModuleTypes;

import java.util.Set;

public class HungerSensor extends Sensor<Villager> {

    private static final int MAX_FOOD_LEVEL = 20;
    private static final int HUNGRY_THRESHOLD = 12;
    private static final int STARVING_THRESHOLD = 6;

    public HungerSensor() {
        super(600);
    }

    @Override
    protected void doTick(ServerLevel level, Villager villager) {
        int foodLevel = villager.getFoodData().getFoodLevel();

        boolean canEat = foodLevel < MAX_FOOD_LEVEL;
        boolean isHungry = foodLevel < HUNGRY_THRESHOLD;
        boolean isStarving = foodLevel < STARVING_THRESHOLD;

        updateMemoryIfChanged(villager, MemoryModuleTypes.CAN_EAT.get(), canEat);
        updateMemoryIfChanged(villager, MemoryModuleTypes.IS_HUNGRY.get(), isHungry);
        updateMemoryIfChanged(villager, MemoryModuleTypes.IS_STARVING.get(), isStarving);

        if (VillageWorks.LOGGER.isDebugEnabled()) {
            VillageWorks.LOGGER.debug("HungerSensor for villager {}: foodLevel={}, canEat={}, isHungry={}, isStarving={}",
                villager.getId(), foodLevel, canEat, isHungry, isStarving);
        }
    }

    private void updateMemoryIfChanged(Villager villager, MemoryModuleType<Boolean> memoryType, boolean newValue) {
        Boolean currentValue = villager.getBrain().getMemory(memoryType).orElse(null);
        if (currentValue == null || !currentValue.equals(newValue)) {
            villager.getBrain().setMemory(memoryType, newValue);
        }
    }

    @Override
    public Set<MemoryModuleType<?>> requires() {
        return ImmutableSet.of(
            MemoryModuleTypes.CAN_EAT.get(),
            MemoryModuleTypes.IS_HUNGRY.get(),
            MemoryModuleTypes.IS_STARVING.get()
        );
    }
}
