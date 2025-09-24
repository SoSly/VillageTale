package org.sosly.villagetale.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import org.jetbrains.annotations.NotNull;
import org.sosly.villagetale.entity.Activities;
import org.sosly.villagetale.entity.MemoryModuleTypes;
import org.sosly.villagetale.entity.Villager;

public class WakeUp extends Behavior<Villager> {
    private static final float DAILY_EXHAUSTION = 24.0f;
    private static final long EXHAUSTION_COOLDOWN = 2000L;

    public WakeUp() {
        super(ImmutableMap.of(
                MemoryModuleTypes.LAST_DAILY_EXHAUSTION.get(), MemoryStatus.REGISTERED
        ));
    }

    @Override
    protected boolean checkExtraStartConditions(@NotNull ServerLevel level, @NotNull Villager villager) {
        if (!villager.getBrain().isActive(Activities.MORNING_IDLE.get())) {
            return false;
        }

        long lastDailyExhaustion = villager.getBrain()
            .getMemory(MemoryModuleTypes.LAST_DAILY_EXHAUSTION.get())
            .orElse(0L);

        return level.getGameTime() - lastDailyExhaustion >= EXHAUSTION_COOLDOWN;
    }

    @Override
    protected void start(@NotNull ServerLevel level, @NotNull Villager villager, long gameTime) {
        villager.getFoodData().addExhaustion(DAILY_EXHAUSTION);
        villager.getBrain().setMemoryWithExpiry(MemoryModuleTypes.LAST_DAILY_EXHAUSTION.get(), gameTime, 24000L);
        villager.stopSleeping();
    }
}
