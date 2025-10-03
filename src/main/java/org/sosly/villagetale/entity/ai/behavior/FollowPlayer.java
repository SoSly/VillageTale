package org.sosly.villagetale.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.player.Player;
import org.sosly.villagetale.entity.MemoryModuleTypes;
import org.sosly.villagetale.entity.Villager;

import java.util.UUID;

public class FollowPlayer extends Behavior<Villager> {
    private static final double FOLLOW_DISTANCE = 3.0D;
    private static final int WALK_PRECISION = 0;
    private final float speedModifier;

    public FollowPlayer(float speedModifier) {
        super(ImmutableMap.of(
            MemoryModuleTypes.FOLLOWING_PLAYER.get(), MemoryStatus.VALUE_PRESENT,
            MemoryModuleType.WALK_TARGET, MemoryStatus.REGISTERED
        ), 200);
        this.speedModifier = speedModifier;
    }

    public static BehaviorControl<Villager> create(float speedModifier) {
        return new FollowPlayer(speedModifier);
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, Villager villager) {
        UUID playerId = villager.getBrain().getMemory(MemoryModuleTypes.FOLLOWING_PLAYER.get()).orElse(null);
        if (playerId == null) {
            return false;
        }

        Player player = level.getPlayerByUUID(playerId);
        if (player == null) {
            return false;
        }

        return !villager.position().closerThan(player.position(), FOLLOW_DISTANCE);
    }

    @Override
    protected void start(ServerLevel level, Villager villager, long gameTime) {
        UUID playerId = villager.getBrain().getMemory(MemoryModuleTypes.FOLLOWING_PLAYER.get()).orElse(null);
        if (playerId == null) {
            return;
        }

        Player player = level.getPlayerByUUID(playerId);
        if (player == null) {
            return;
        }

        villager.getBrain().setMemory(MemoryModuleType.WALK_TARGET,
            new WalkTarget(player, speedModifier, WALK_PRECISION));
    }

    @Override
    protected boolean canStillUse(ServerLevel level, Villager villager, long gameTime) {
        UUID playerId = villager.getBrain().getMemory(MemoryModuleTypes.FOLLOWING_PLAYER.get()).orElse(null);
        if (playerId == null) {
            return false;
        }

        Player player = level.getPlayerByUUID(playerId);
        if (player == null) {
            return false;
        }

        return !villager.position().closerThan(player.position(), FOLLOW_DISTANCE);
    }

    @Override
    protected void tick(ServerLevel level, Villager villager, long gameTime) {
        UUID playerId = villager.getBrain().getMemory(MemoryModuleTypes.FOLLOWING_PLAYER.get()).orElse(null);
        if (playerId == null) {
            return;
        }

        Player player = level.getPlayerByUUID(playerId);
        if (player == null) {
            return;
        }

        villager.getBrain().setMemory(MemoryModuleType.WALK_TARGET,
            new WalkTarget(player, speedModifier, WALK_PRECISION));
    }

    @Override
    protected void stop(ServerLevel level, Villager villager, long gameTime) {
        villager.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
    }
}
