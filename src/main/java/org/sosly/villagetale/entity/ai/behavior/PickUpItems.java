package org.sosly.villagetale.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.InventoryCarrier;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.sosly.villagetale.api.IVillageZone;
import org.sosly.villagetale.entity.MemoryModuleTypes;
import org.sosly.villagetale.entity.Villager;
import org.sosly.villagetale.helper.InventoryHelper;
import org.sosly.villagetale.helper.VillagesHelper;

public class PickUpItems extends Behavior<Villager> {
    private static final double SCAN_RADIUS = 16.0D;
    private static final double COLLECTION_DISTANCE = 3.0D;
    private static final int SCAN_INTERVAL = 20;
    private static final int BEHAVIOR_DURATION = 40;

    private ItemEntity targetItem;
    private int scanCooldown;

    public PickUpItems() {
        super(ImmutableMap.of(
            MemoryModuleTypes.WORK_ZONE.get(), MemoryStatus.VALUE_PRESENT
        ), BEHAVIOR_DURATION);
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, Villager villager) {
        if (scanCooldown > 0) {
            scanCooldown--;
            return false;
        }

        scanCooldown = SCAN_INTERVAL;

        IVillageZone zone = getWorkZone(level, villager);
        if (zone == null) {
            return false;
        }

        AABB searchBox = new AABB(villager.blockPosition()).inflate(SCAN_RADIUS);
        List<ItemEntity> items = level.getEntitiesOfClass(ItemEntity.class, searchBox,
            item -> !item.isRemoved() && item.isAlive()
                && zone.containsPosition(item.blockPosition())
                && InventoryHelper.canAddToInventory(villager.getInventory(), item.getItem()));

        if (items.isEmpty()) {
            return false;
        }

        // Debug: Check for multiple items at same position
        if (items.size() > 1) {
            BlockPos firstPos = items.get(0).blockPosition();
            long samePos = items.stream().filter(i -> i.blockPosition().equals(firstPos)).count();
            if (samePos > 1) {
                System.out.println("PickUpItems: Found " + samePos + " items at position " + firstPos);
            }
        }

        targetItem = items.stream()
            .min(Comparator.comparingDouble(a -> a.distanceToSqr(villager)))
            .orElse(null);

        return targetItem != null;
    }

    @Override
    protected void start(ServerLevel level, Villager villager, long gameTime) {
        if (targetItem == null || targetItem.isRemoved()) {
            return;
        }

        Vec3 targetPos = targetItem.position();
        villager.getBrain().setMemory(MemoryModuleType.WALK_TARGET,
            new WalkTarget(targetPos, 0.5F, 3));
    }

    @Override
    protected void stop(ServerLevel level, Villager villager, long gameTime) {
        villager.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
        targetItem = null;
        scanCooldown = 0;
    }

    @Override
    protected boolean canStillUse(ServerLevel level, Villager villager, long gameTime) {
        if (targetItem == null || targetItem.isRemoved() || !targetItem.isAlive()) {
            return false;
        }

        return true;
    }

    @Override
    protected void tick(ServerLevel level, Villager villager, long gameTime) {
        if (targetItem == null || targetItem.isRemoved()) {
            return;
        }

        if (!targetItem.position().closerThan(villager.blockPosition().getCenter(), COLLECTION_DISTANCE)) {
            villager.getBrain().setMemory(MemoryModuleType.WALK_TARGET,
                new WalkTarget(targetItem.position(), 0.5F, 3));
            return;
        }

        villager.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
        InventoryCarrier.pickUpItem(villager, villager, targetItem);

        targetItem = null;
    }

    private IVillageZone getWorkZone(ServerLevel level, Villager villager) {
        if (villager.getVillage().isEmpty()) {
            return null;
        }

        UUID villageId = villager.getVillage().get();
        UUID workplaceId = villager.getBrain().getMemory(MemoryModuleTypes.WORK_ZONE.get()).orElse(null);
        if (workplaceId == null) {
            return null;
        }

        return VillagesHelper.getWorkZone(level, villager, villageId, workplaceId);
    }
}
