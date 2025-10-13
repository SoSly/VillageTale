package org.sosly.villagetale.entity.ai.sensor;

import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.sosly.villagetale.api.IVillageZone;
import org.sosly.villagetale.api.IWantedItem;
import org.sosly.villagetale.config.CommonConfig;
import org.sosly.villagetale.entity.MemoryModuleTypes;
import org.sosly.villagetale.entity.Villager;
import org.sosly.villagetale.helper.InventoryHelper;
import org.sosly.villagetale.helper.VillagesHelper;
import org.sosly.villagetale.zone.type.Dock;

public class IsFishableWater extends Sensor<Villager> {
    public IsFishableWater() {
        super(100);
    }

    @Override
    protected void doTick(@NotNull ServerLevel level, Villager villager) {
        if (villager.getVillage().isEmpty() || !villager.getBrain().hasMemoryValue(MemoryModuleTypes.WORK_ZONE.get())) {
            return;
        }

        UUID workplaceId = villager.getBrain().getMemory(MemoryModuleTypes.WORK_ZONE.get()).get();
        IVillageZone zone = VillagesHelper.getZoneById(level, villager.getVillage().get(), workplaceId);
        if (zone == null) {
            return;
        }

        if (!zone.containsPosition(villager.blockPosition())) {
            return;
        }

        if (!(zone.getType() instanceof Dock)) {
            return;
        }

        findNearestFishingSpot(level, villager, zone);
    }

    private void findNearestFishingSpot(ServerLevel level, Villager villager, IVillageZone zone) {
        List<IWantedItem> tools = villager.getProfession().getTools();
        if (tools.isEmpty()) {
            return;
        }

        ItemStack tool = InventoryHelper.getItem(villager, stack -> tools.get(0).getMatcher().test(stack));
        if (tool.isEmpty()) {
            return;
        }

        BlockPos villagerPos = villager.blockPosition();
        int scanRadius = (int) CommonConfig.scanRadius;
        double nearestDistance = Double.MAX_VALUE;
        double minDistance = 4.0;
        List<BlockPos> fishable = new ArrayList<>();

        for (int x = -scanRadius; x <= scanRadius; x++) {
            for (int y = -10; y <= 0; y++) {
                for (int z = -scanRadius; z <= scanRadius; z++) {
                    BlockPos pos = villagerPos.offset(x, y, z);

                    BlockState state = level.getBlockState(pos);
                    if (!state.is(Blocks.WATER)) {
                        continue;
                    }

                    BlockState above = level.getBlockState(pos.above());
                    if (!above.isAir()) {
                        continue;
                    }

                    if (!hasLineOfSight(level, villager, pos)) {
                        continue;
                    }

                    double distance = villagerPos.distSqr(pos);
                    if (distance >= minDistance && distance < nearestDistance) {
                        nearestDistance = distance;
                        fishable.add(pos);
                    }
                }
            }
        }

        if (fishable.isEmpty()) {
            return;
        }

        BlockPos fishingPos = fishable.get(villager.getRandom().nextInt(fishable.size()));
        villager.getBrain().setMemoryWithExpiry(MemoryModuleTypes.NEAREST_FISHING_SPOT.get(), fishingPos, 600L);
    }

    private boolean hasLineOfSight(ServerLevel level, Villager villager, BlockPos waterPos) {
        Vec3 eyePos = villager.getEyePosition();
        Vec3 waterCenter = new Vec3(waterPos.getX() + 0.5, waterPos.getY() + 0.5, waterPos.getZ() + 0.5);

        ClipContext context = new ClipContext(
            eyePos,
            waterCenter,
            ClipContext.Block.COLLIDER,
            ClipContext.Fluid.NONE,
            villager
        );

        BlockHitResult result = level.clip(context);
        return result.getType() == HitResult.Type.MISS || result.getBlockPos().equals(waterPos);
    }

    @Override
    public @NotNull Set<MemoryModuleType<?>> requires() {
        return ImmutableSet.of(
            MemoryModuleTypes.WORK_ZONE.get(),
            MemoryModuleTypes.NEAREST_FISHING_SPOT.get()
        );
    }
}
