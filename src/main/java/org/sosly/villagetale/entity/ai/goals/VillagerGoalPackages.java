package org.sosly.villagetale.entity.ai.goals;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.behavior.DoNothing;
import net.minecraft.world.entity.ai.behavior.InteractWithDoor;
import net.minecraft.world.entity.ai.behavior.LookAtTargetSink;
import net.minecraft.world.entity.ai.behavior.MoveToTargetSink;
import net.minecraft.world.entity.ai.behavior.RandomStroll;
import net.minecraft.world.entity.ai.behavior.RunOne;
import net.minecraft.world.entity.ai.behavior.SetEntityLookTarget;
import net.minecraft.world.entity.ai.behavior.SetWalkTargetAwayFrom;
import net.minecraft.world.entity.ai.behavior.SetWalkTargetFromLookTarget;
import net.minecraft.world.entity.ai.behavior.SleepInBed;
import net.minecraft.world.entity.ai.behavior.Swim;
import net.minecraft.world.entity.ai.behavior.UpdateActivityFromSchedule;
import net.minecraft.world.entity.ai.behavior.VillageBoundRandomStroll;
import net.minecraft.world.entity.ai.behavior.VillagerCalmDown;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import org.sosly.villagetale.entity.MemoryModuleTypes;
import org.sosly.villagetale.entity.Villager;
import org.sosly.villagetale.entity.ai.behavior.DepositItem;
import org.sosly.villagetale.entity.ai.behavior.EatFood;
import org.sosly.villagetale.entity.ai.behavior.GetFromContainer;
import org.sosly.villagetale.entity.ai.behavior.GoToAssignedBed;
import org.sosly.villagetale.entity.ai.behavior.GoToNearestStorage;
import org.sosly.villagetale.entity.ai.behavior.GoToWorkZone;
import org.sosly.villagetale.entity.ai.behavior.SetWalkTargetFromBlockMemory;
import org.sosly.villagetale.entity.ai.behavior.VillagerPanicTrigger;
import org.sosly.villagetale.entity.ai.behavior.WakeUp;
import org.sosly.villagetale.entity.ai.behavior.ZoneBoundRandomStroll;

public class VillagerGoalPackages {

    @SuppressWarnings("unchecked")
    public static ImmutableList<Pair<Integer, ? extends BehaviorControl<? super Villager>>> getCorePackage() {
        return ImmutableList.of(
            Pair.of(0, (BehaviorControl<? super Villager>) new Swim(0.8F)),
            Pair.of(0, (BehaviorControl<? super Villager>) InteractWithDoor.create()),
            Pair.of(0, (BehaviorControl<? super Villager>) new LookAtTargetSink(45, 90)),
            Pair.of(0, VillagerPanicTrigger.create()),
            Pair.of(1, (BehaviorControl<? super Villager>) new MoveToTargetSink()),
            Pair.of(1, WakeUp.create()),
            Pair.of(2, (BehaviorControl<? super Villager>) new EatFood()),
            Pair.of(3, (BehaviorControl<? super Villager>) new GetFromContainer()),
            Pair.of(3, (BehaviorControl<? super Villager>) new DepositItem()),
            Pair.of(99, (BehaviorControl<? super Villager>) UpdateActivityFromSchedule.create())
        );
    }

    @SuppressWarnings("unchecked")
    public static ImmutableList<Pair<Integer, ? extends BehaviorControl<? super Villager>>> getIdlePackage(float speedModifier) {
        return ImmutableList.of(
            Pair.of(3, (BehaviorControl<? super Villager>) new GoToNearestStorage()),
            Pair.of(5, (BehaviorControl<? super Villager>) new RunOne<>(ImmutableList.of(
                    Pair.of(RandomStroll.stroll(speedModifier), 2),
                    Pair.of(SetWalkTargetFromLookTarget.create(speedModifier, 3), 2),
                Pair.of(new DoNothing(30, 60), 1)
            ))),
            getMinimalLookBehavior()
        );
    }

    @SuppressWarnings("unchecked")
    public static ImmutableList<Pair<Integer, ? extends BehaviorControl<? super Villager>>> getWorkPackage(float speedModifier) {
        return ImmutableList.of(
                Pair.of(5, (BehaviorControl<? super Villager>) new GoToNearestStorage()),
                Pair.of(10, SetWalkTargetFromBlockMemory.create(MemoryModuleTypes.WORK_POS.get(), speedModifier, 3, 100, 1200)),
                Pair.of(15, (BehaviorControl<? super Villager>) new RunOne<>(ImmutableList.of(
                        Pair.of(ZoneBoundRandomStroll.create(speedModifier), 2),
                        Pair.of(SetWalkTargetFromLookTarget.create(speedModifier, 3), 2),
                        Pair.of(new DoNothing(30, 60), 1)
                ))),
                Pair.of(99, (BehaviorControl<? super Villager>) new GoToWorkZone()),
                getMinimalLookBehavior()
        );
    }

    @SuppressWarnings("unchecked")
    public static ImmutableList<Pair<Integer, ? extends BehaviorControl<? super Villager>>> getRestPackage(float speedModifier) {
        return ImmutableList.of(
            Pair.of(2, new SleepInBed()),
            Pair.of(3, GoToAssignedBed.create(speedModifier)),
            Pair.of(4, SetWalkTargetFromBlockMemory.create(MemoryModuleType.HOME, speedModifier, 1, 150, 1200)),
            getMinimalLookBehavior(),
            Pair.of(99, (BehaviorControl<? super Villager>) UpdateActivityFromSchedule.create())
        );
    }

    @SuppressWarnings("unchecked")
    private static Pair<Integer, BehaviorControl<? super Villager>> getMinimalLookBehavior() {
        return Pair.of(5, (BehaviorControl<? super Villager>) new RunOne<>(ImmutableList.of(
            Pair.of(SetEntityLookTarget.create(net.minecraft.world.entity.EntityType.PLAYER, 8.0F), 2),
            Pair.of(new DoNothing(30, 60), 8)
        )));
    }

    @SuppressWarnings("unchecked")
    public static ImmutableList<Pair<Integer, ? extends BehaviorControl<? super Villager>>> getPanicPackage(float speedModifier) {
        float panicSpeed = speedModifier * 1.5F;
        return ImmutableList.of(
            Pair.of(0, (BehaviorControl<? super Villager>) VillagerCalmDown.create()),
            Pair.of(0, VillagerPanicTrigger.create()),
            Pair.of(1, (BehaviorControl<? super Villager>) SetWalkTargetAwayFrom.entity(MemoryModuleType.NEAREST_HOSTILE, panicSpeed, 6, false)),
            Pair.of(1, (BehaviorControl<? super Villager>) SetWalkTargetAwayFrom.entity(MemoryModuleType.HURT_BY_ENTITY, panicSpeed, 6, false)),
            Pair.of(3, (BehaviorControl<? super Villager>) VillageBoundRandomStroll.create(panicSpeed, 2, 2)),
            getMinimalLookBehavior()
        );
    }
}
