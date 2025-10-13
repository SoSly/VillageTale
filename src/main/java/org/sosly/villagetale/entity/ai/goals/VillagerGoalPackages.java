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
import org.sosly.villagetale.entity.ai.behavior.CloseOpenedGates;
import org.sosly.villagetale.entity.ai.behavior.EatFood;
import org.sosly.villagetale.entity.ai.behavior.FollowPlayer;
import org.sosly.villagetale.entity.ai.behavior.GetFromContainer;
import org.sosly.villagetale.entity.ai.behavior.GoToAssignedBed;
import org.sosly.villagetale.entity.ai.behavior.GoToNearestStorage;
import org.sosly.villagetale.entity.ai.behavior.GoToWorkZone;
import org.sosly.villagetale.entity.ai.behavior.InteractWithGate;
import org.sosly.villagetale.entity.ai.behavior.PickUpItems;
import org.sosly.villagetale.entity.ai.behavior.PutInContainer;
import org.sosly.villagetale.entity.ai.behavior.SetWalkTargetFromBlockMemory;
import org.sosly.villagetale.entity.ai.behavior.VillagerPanicTrigger;
import org.sosly.villagetale.entity.ai.behavior.WakeUp;
import org.sosly.villagetale.entity.ai.behavior.ZoneBoundRandomStroll;
import net.minecraft.world.entity.EntityType;

public class VillagerGoalPackages {

    @SuppressWarnings("unchecked")
    public static ImmutableList<Pair<Integer, ? extends BehaviorControl<? super Villager>>> getCorePackage() {
        return ImmutableList.of(
            Pair.of(0, new Swim(0.8F)),
            Pair.of(0, InteractWithDoor.create()),
            Pair.of(0, InteractWithGate.create()),
            Pair.of(0, CloseOpenedGates.create()),
            Pair.of(0, new LookAtTargetSink(45, 90)),
            Pair.of(0, VillagerPanicTrigger.create()),
            Pair.of(0, FollowPlayer.create(0.6F)),
            Pair.of(1, new MoveToTargetSink()),
            Pair.of(1, new WakeUp()),
            Pair.of(2, new EatFood()),
            Pair.of(3, new PickUpItems()),
            Pair.of(3, new PutInContainer()),
            Pair.of(4, new GetFromContainer()),
            Pair.of(99, UpdateActivityFromSchedule.create())
        );
    }

    @SuppressWarnings("unchecked")
    public static ImmutableList<Pair<Integer, ? extends BehaviorControl<? super Villager>>> getMorningIdlePackage(float speedModifier) {
        return ImmutableList.of(
            Pair.of(3, new GoToNearestStorage()),
            Pair.of(5, new RunOne<>(ImmutableList.of(
                    Pair.of(RandomStroll.stroll(speedModifier), 2),
                    Pair.of(SetWalkTargetFromLookTarget.create(speedModifier, 3), 2),
                Pair.of(new DoNothing(30, 60), 1)
            ))),
            getMinimalLookBehavior()
        );
    }

    @SuppressWarnings("unchecked")
    public static ImmutableList<Pair<Integer, ? extends BehaviorControl<? super Villager>>> getEveningIdlePackage(float speedModifier) {
        return ImmutableList.of(
            Pair.of(3, new GoToNearestStorage()),
            Pair.of(5, new RunOne<>(ImmutableList.of(
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
                Pair.of(5, new GoToNearestStorage()),
                Pair.of(10, SetWalkTargetFromBlockMemory.create(MemoryModuleTypes.WORK_POS.get(), speedModifier, 3, 100, 1200)),
                Pair.of(15, new RunOne<>(ImmutableList.of(
                        Pair.of(ZoneBoundRandomStroll.create(speedModifier), 2),
                        Pair.of(SetWalkTargetFromLookTarget.create(speedModifier, 3), 2),
                        Pair.of(new DoNothing(30, 60), 1)
                ))),
                Pair.of(99, new GoToWorkZone()),
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
            Pair.of(99, UpdateActivityFromSchedule.create())
        );
    }

    @SuppressWarnings("unchecked")
    private static Pair<Integer, BehaviorControl<? super Villager>> getMinimalLookBehavior() {
        return Pair.of(5, new RunOne<>(ImmutableList.of(
            Pair.of(SetEntityLookTarget.create(EntityType.PLAYER, 8.0F), 2),
            Pair.of(new DoNothing(30, 60), 8)
        )));
    }

    @SuppressWarnings("unchecked")
    public static ImmutableList<Pair<Integer, ? extends BehaviorControl<? super Villager>>> getPanicPackage(float speedModifier) {
        float panicSpeed = speedModifier * 1.5F;
        return ImmutableList.of(
            Pair.of(0, VillagerCalmDown.create()),
            Pair.of(0, VillagerPanicTrigger.create()),
            Pair.of(1, SetWalkTargetAwayFrom.entity(MemoryModuleType.NEAREST_HOSTILE, panicSpeed, 6, false)),
            Pair.of(1, SetWalkTargetAwayFrom.entity(MemoryModuleType.HURT_BY_ENTITY, panicSpeed, 6, false)),
            Pair.of(3, VillageBoundRandomStroll.create(panicSpeed, 2, 2)),
            getMinimalLookBehavior()
        );
    }
}
