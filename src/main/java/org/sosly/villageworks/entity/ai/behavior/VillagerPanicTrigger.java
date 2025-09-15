package org.sosly.villageworks.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.schedule.Activity;
import org.sosly.villageworks.entity.Villager;

public class VillagerPanicTrigger extends Behavior<Villager> {
    
    public VillagerPanicTrigger() {
        super(ImmutableMap.of());
    }
    
    public static VillagerPanicTrigger create() {
        return new VillagerPanicTrigger();
    }
    
    @Override
    protected boolean canStillUse(ServerLevel level, Villager villager, long gameTime) {
        return isHurt(villager) || hasHostile(villager);
    }
    
    @Override
    protected void start(ServerLevel level, Villager villager, long gameTime) {
        if (!isHurt(villager) && !hasHostile(villager)) {
            return;
        }
        
        Brain<Villager> brain = villager.getBrain();
        if (!brain.isActive(Activity.PANIC)) {
            brain.eraseMemory(MemoryModuleType.PATH);
            brain.eraseMemory(MemoryModuleType.WALK_TARGET);
            brain.eraseMemory(MemoryModuleType.LOOK_TARGET);
        }
        brain.setActiveActivityIfPossible(Activity.PANIC);
    }
    
    private static boolean isHurt(Villager villager) {
        return villager.getBrain().hasMemoryValue(MemoryModuleType.HURT_BY);
    }
    
    private static boolean hasHostile(Villager villager) {
        return villager.getBrain().hasMemoryValue(MemoryModuleType.NEAREST_HOSTILE);
    }
}