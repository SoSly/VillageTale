package org.sosly.villagetale.entity.ai.behavior;

import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.schedule.Activity;
import org.sosly.villagetale.entity.Villager;

public class WakeUp {
    public static BehaviorControl<Villager> create() {
        return BehaviorBuilder.create(instance ->
            instance.point((level, villager, gameTime) -> {
                if (!villager.getBrain().isActive(Activity.REST) && villager.isSleeping()) {
                    villager.stopSleeping();
                    return true;
                }
                return false;
            })
        );
    }
}
