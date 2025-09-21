package org.sosly.villagetale.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ToolActions;
import org.sosly.villagetale.api.IVillageZone;
import org.sosly.villagetale.config.CommonConfig;
import org.sosly.villagetale.entity.MemoryModuleTypes;
import org.sosly.villagetale.entity.Villager;
import org.sosly.villagetale.helper.VillagesHelper;
import org.sosly.villagetale.network.NetworkHandler;

public class TillSoil extends Behavior<Villager> {
    private static final int TILLING_DURATION = 40;
    private static final int BEHAVIOR_DURATION = 100;

    boolean claimed;
    BlockPos pos;
    int tillTicks;

    public TillSoil() {
        super(ImmutableMap.of(
                MemoryModuleTypes.WORK_ZONE.get(), MemoryStatus.VALUE_PRESENT,
                MemoryModuleTypes.NEAREST_TILLABLE_SOIL.get(), MemoryStatus.VALUE_PRESENT,
                MemoryModuleTypes.BUSY.get(), MemoryStatus.VALUE_ABSENT
        ), BEHAVIOR_DURATION);
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, Villager villager) {
ItemStack tool = getTool(villager);
        if (tool.isEmpty()) {
            return false;
        }

        IVillageZone zone = getZone(level, villager);
        if (zone == null) {
            return false;
        }

        return zone.containsPosition(villager.blockPosition());
    }

    @Override
    protected void start(ServerLevel level, Villager villager, long gameTime) {
        IVillageZone zone = getZone(level, villager);
        if (zone == null) {
            return;
        }

        pos = villager.getBrain().getMemory(MemoryModuleTypes.NEAREST_TILLABLE_SOIL.get()).orElse(null);
        if (pos == null) {
            return;
        }

        claimed = zone.claim(pos, villager.getUUID(), BEHAVIOR_DURATION, gameTime);
        if (!claimed) {
            return;
        }
        villager.getBrain().setMemoryWithExpiry(MemoryModuleTypes.BUSY.get(), true, BEHAVIOR_DURATION);
        ItemStack tool = getTool(villager);
        villager.setItemInHand(InteractionHand.MAIN_HAND, tool);
        NetworkHandler.syncEquipmentToNearbyPlayers(villager, InteractionHand.MAIN_HAND, tool);

        if (villager.blockPosition().closerThan(pos, CommonConfig.interactionDistance)) {
            return;
        }
        villager.getBrain().setMemory(MemoryModuleType.WALK_TARGET,
            new WalkTarget(pos, 0.5F, 1));
    }

    @Override
    protected void stop(ServerLevel level, Villager villager, long gameTime) {
        villager.getBrain().eraseMemory(MemoryModuleTypes.BUSY.get());
        villager.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        NetworkHandler.syncEquipmentToNearbyPlayers(villager, InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        pos = null;
        tillTicks = 0;
    }

    @Override
    protected boolean canStillUse(ServerLevel level, Villager villager, long gameTime) {
        return claimed;
    }

    @Override
    protected void tick(ServerLevel level, Villager villager, long gameTime) {
        if (!claimed || pos == null) {
            return;
        }

        if (!villager.blockPosition().closerThan(pos, CommonConfig.interactionDistance)) {
            return;
        }

        villager.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
        if (tillTicks++ < TILLING_DURATION) {
            if (tillTicks % 10 == 0) {
                villager.getLookControl().setLookAt(
                    pos.getX() + 0.5,
                    pos.getY() + 0.5,
                    pos.getZ() + 0.5
                );
                villager.swing(villager.getUsedItemHand());
            }
            return;
        }

        BlockState currentState = level.getBlockState(pos);
        ItemStack tool = getTool(villager);
        BlockState tilledState = currentState.getToolModifiedState(
            new UseOnContext(level, null, InteractionHand.MAIN_HAND, tool,
                new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false)),
            ToolActions.HOE_TILL, false);

        if (tilledState != null) {
            level.setBlock(pos, tilledState, 3);
            level.playSound(null, pos, SoundEvents.HOE_TILL, SoundSource.BLOCKS, 1.0F, 1.0F);
        }

        villager.getBrain().eraseMemory(MemoryModuleTypes.NEAREST_TILLABLE_SOIL.get());

        tool.setDamageValue(tool.getDamageValue() + 1);
        if (tool.getDamageValue() >= tool.getMaxDamage()) {
            tool.shrink(1);
            level.playSound(null, pos, SoundEvents.ITEM_BREAK, SoundSource.NEUTRAL, 1.0F, 1.0F);
        }

        claimed = false;
    }

    private ItemStack getTool(Villager villager) {
        Container inventory = villager.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            if (inventory.getItem(i).is(ItemTags.HOES)) {
                return inventory.getItem(i);
            }
        }
        return ItemStack.EMPTY;
    }

    private IVillageZone getZone(ServerLevel level, Villager villager) {
        if (villager.getVillage().isEmpty()) {
            return null;
        }

        UUID villageId = villager.getVillage().get();
        UUID workplaceId = villager.getBrain().getMemory(MemoryModuleTypes.WORK_ZONE.get()).orElse(null);
        if (workplaceId == null) {
            return null;
        }

        IVillageZone zone = VillagesHelper.getZoneById(level, villageId, workplaceId);
        if (zone == null) {
            return null;
        }

        return zone;
    }
}
