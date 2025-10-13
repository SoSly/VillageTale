package org.sosly.villagetale.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.phys.Vec3;
import org.sosly.villagetale.api.IVillageZone;
import org.sosly.villagetale.entity.MemoryModuleTypes;
import org.sosly.villagetale.entity.Villager;
import org.sosly.villagetale.helper.VillagesHelper;

public class ZoneBoundRandomStroll extends Behavior<Villager> {
    private static final int MAX_ATTEMPTS = 10;
    private static final int STROLL_DISTANCE = 10;
    private static final int BEHAVIOR_DURATION = 200;
    private final float speedModifier;
    private final int maxHorizontalDistance;
    private final int maxVerticalDistance;
    
    public ZoneBoundRandomStroll(float speedModifier) {
        this(speedModifier, STROLL_DISTANCE, 3);
    }
    
    public ZoneBoundRandomStroll(float speedModifier, int maxHorizontalDistance, int maxVerticalDistance) {
        super(ImmutableMap.of(
                MemoryModuleTypes.VILLAGE.get(), MemoryStatus.VALUE_PRESENT,
                MemoryModuleTypes.WORK_ZONE.get(), MemoryStatus.VALUE_PRESENT,
                MemoryModuleType.WALK_TARGET, MemoryStatus.VALUE_ABSENT
        ), BEHAVIOR_DURATION);
        this.speedModifier = speedModifier;
        this.maxHorizontalDistance = maxHorizontalDistance;
        this.maxVerticalDistance = maxVerticalDistance;
    }
    
    public static ZoneBoundRandomStroll create(float speedModifier) {
        return new ZoneBoundRandomStroll(speedModifier);
    }
    
    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, Villager villager) {
        Optional<UUID> villageId = villager.getVillage();
        Optional<UUID> workZoneId = villager.getBrain().getMemory(MemoryModuleTypes.WORK_ZONE.get());
        
        if (villageId.isEmpty() || workZoneId.isEmpty()) {
            return false;
        }
        
        IVillageZone zone = VillagesHelper.getZoneById(level, villageId.get(), workZoneId.get());
        if (zone == null) {
            return false;
        }
        
        return zone.containsPosition(villager.blockPosition());
    }
    
    @Override
    protected void start(ServerLevel level, Villager villager, long gameTime) {
        UUID villageId = villager.getVillage().get();
        UUID workZoneId = villager.getBrain().getMemory(MemoryModuleTypes.WORK_ZONE.get()).get();
        
        IVillageZone zone = VillagesHelper.getZoneById(level, villageId, workZoneId);
        if (zone == null) {
            return;
        }
        
        Optional<BlockPos> targetPos = findValidStrollPosition(villager, zone);
        
        if (targetPos.isPresent()) {
            villager.getBrain().setMemoryWithExpiry(MemoryModuleType.WALK_TARGET,
                new WalkTarget(targetPos.get(), this.speedModifier, 0), 200L);
        } else {
            BlockPos startPos = zone.getStartPosition();
            if (!villager.blockPosition().equals(startPos)) {
                villager.getBrain().setMemoryWithExpiry(MemoryModuleType.WALK_TARGET,
                    new WalkTarget(startPos, this.speedModifier, 2), 200L);
            }
        }
    }
    
    private Optional<BlockPos> findValidStrollPosition(Villager villager, IVillageZone zone) {
        for (int i = 0; i < MAX_ATTEMPTS; i++) {
            Vec3 randomPos = LandRandomPos.getPos(villager, maxHorizontalDistance, maxVerticalDistance);
            if (randomPos != null) {
                BlockPos targetPos = BlockPos.containing(randomPos);
                if (zone.containsPosition(targetPos)) {
                    return Optional.of(targetPos);
                }
            }
        }
        return Optional.empty();
    }
}
