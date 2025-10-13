package org.sosly.villagetale.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import org.jetbrains.annotations.NotNull;
import org.sosly.villagetale.api.IVillageZone;
import org.sosly.villagetale.api.IWantedItem;
import org.sosly.villagetale.config.CommonConfig;
import org.sosly.villagetale.entity.EntityTypes;
import org.sosly.villagetale.entity.FishingBobber;
import org.sosly.villagetale.entity.MemoryModuleTypes;
import org.sosly.villagetale.entity.Villager;
import org.sosly.villagetale.helper.InventoryHelper;
import org.sosly.villagetale.helper.VillagesHelper;
import org.sosly.villagetale.network.packets.clientbound.VillagerEquipmentSync;

public class CatchFish extends Behavior<Villager> {
    private static final int MINIMUM_DURATION = 300;
    private static final int MAXIMUM_DURATION = 300;
    private static final int BEHAVIOR_DURATION = 600;
    private static final float WORK_EXHAUSTION = 0.5f;

    private BlockPos fishingSpot;
    private BlockPos standingSpot;
    private int fishTicks;
    private int fishDuration;
    private ItemStack rod = ItemStack.EMPTY;
    private IVillageZone workplace;
    private boolean finished;
    private FishingBobber bobber;

    public CatchFish() {
        super(ImmutableMap.of(
                MemoryModuleTypes.WORK_ZONE.get(), MemoryStatus.VALUE_PRESENT,
                MemoryModuleTypes.NEAREST_FISHING_SPOT.get(), MemoryStatus.VALUE_PRESENT,
                MemoryModuleTypes.BUSY.get(), MemoryStatus.VALUE_ABSENT
        ), BEHAVIOR_DURATION);
    }

    @Override
    protected boolean checkExtraStartConditions(@NotNull ServerLevel level, @NotNull Villager villager) {
        List<IWantedItem> tools = villager.getProfession().getTools();
        if (tools.isEmpty()) {
            return false;
        }

        ItemStack tool = InventoryHelper.getItem(villager, stack -> tools.get(0).getMatcher().test(stack));
        if (tool.isEmpty()) {
            return false;
        }

        IVillageZone zone = VillagesHelper.getWorkplaceZone(level, villager);
        if (zone == null) {
            return false;
        }

        if (!zone.containsPosition(villager.blockPosition())) {
            return false;
        }

        this.rod = tool;
        this.workplace = zone;
        this.fishingSpot = villager.getBrain().getMemory(MemoryModuleTypes.NEAREST_FISHING_SPOT.get())
                .orElseThrow();
        this.standingSpot = findBestStandingSpot(level, villager);

        if (this.standingSpot == null) {
            return false;
        }

        return true;
    }

    @Override
    protected void start(@NotNull ServerLevel level, Villager villager, long gameTime) {
        int range = MAXIMUM_DURATION - MINIMUM_DURATION;
        this.fishDuration = range > 0 ? MINIMUM_DURATION + level.random.nextInt(range) : MINIMUM_DURATION;
        this.finished = false;

        villager.getBrain().setMemoryWithExpiry(MemoryModuleTypes.BUSY.get(), true, BEHAVIOR_DURATION);
        villager.setItemInHand(InteractionHand.MAIN_HAND, rod);
        VillagerEquipmentSync.sendToNearbyPlayers(villager, InteractionHand.MAIN_HAND, rod);

        if (!villager.blockPosition().closerThan(this.standingSpot, 2)) {
            villager.getBrain().setMemoryWithExpiry(MemoryModuleType.WALK_TARGET,
                new WalkTarget(this.standingSpot, 0.5F, 0), BEHAVIOR_DURATION);
        }
    }

    @Override
    protected void stop(@NotNull ServerLevel level, Villager villager, long gameTime) {
        villager.getBrain().eraseMemory(MemoryModuleTypes.BUSY.get());
        villager.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        VillagerEquipmentSync.sendToNearbyPlayers(villager, InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        villager.getBrain().eraseMemory(MemoryModuleTypes.NEAREST_FISHING_SPOT.get());
        villager.setFishingPos(null);

        if (this.bobber != null && !this.bobber.isRemoved()) {
            this.bobber.discard();
        }

        this.fishingSpot = null;
        this.standingSpot = null;
        this.fishTicks = 0;
        this.fishDuration = 0;
        this.rod = ItemStack.EMPTY;
        this.workplace = null;
        this.finished = false;
        this.bobber = null;
    }

    @Override
    protected boolean canStillUse(@NotNull ServerLevel level, @NotNull Villager villager, long gameTime) {
        return !finished;
    }

    @Override
    protected void tick(@NotNull ServerLevel level, @NotNull Villager villager, long gameTime) {
        if (!villager.blockPosition().closerThan(this.standingSpot, 2)) {
            villager.getBrain().setMemoryWithExpiry(MemoryModuleType.WALK_TARGET,
                    new WalkTarget(this.standingSpot, 0.5F, 0), BEHAVIOR_DURATION);
            return;
        }

        villager.getLookControl().setLookAt(
                fishingSpot.getX() + 0.5,
                fishingSpot.getY() + 0.5,
                fishingSpot.getZ() + 0.5
        );

        if (this.bobber == null || this.bobber.isRemoved()) {
            villager.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
            villager.setFishingPos(fishingSpot);
            this.bobber = new FishingBobber(EntityTypes.FISHING_BOBBER.get(), level);
            this.bobber.setOwner(villager);
            this.bobber.setPos(fishingSpot.getX() + 0.5, fishingSpot.getY() + 0.9, fishingSpot.getZ() + 0.5);
            level.addFreshEntity(this.bobber);
            level.playSound(null, villager.blockPosition(), SoundEvents.FISHING_BOBBER_THROW, SoundSource.NEUTRAL, 0.5F, 0.4F / (level.random.nextFloat() * 0.4F + 0.8F));
            return;
        }

        if (this.fishTicks == 0) {
            level.playSound(null, fishingSpot, SoundEvents.FISHING_BOBBER_SPLASH, SoundSource.NEUTRAL, 0.25F, 1.0F + (level.random.nextFloat() - level.random.nextFloat()) * 0.4F);
        }

        if (this.fishTicks == this.fishDuration - 20) {
            level.playSound(null, fishingSpot, SoundEvents.FISHING_BOBBER_SPLASH, SoundSource.NEUTRAL, 0.5F, 1.2F + (level.random.nextFloat() - level.random.nextFloat()) * 0.4F);
        }

        if (fishTicks++ < fishDuration) {
            return;
        }

        villager.setFishingPos(null);
        generateFishingLoot(level, villager);

        villager.getFoodData().addExhaustion(WORK_EXHAUSTION);
        rod.setDamageValue(rod.getDamageValue() + 1);
        if (rod.getDamageValue() >= rod.getMaxDamage()) {
            rod.shrink(1);
            float volume = 0.9F + level.random.nextFloat() * 0.2F;
            float pitch = 0.9F + level.random.nextFloat() * 0.2F;
            level.playSound(null, villager.blockPosition(), SoundEvents.ITEM_BREAK, SoundSource.NEUTRAL, volume, pitch);
        }

        this.finished = true;
    }

    private void generateFishingLoot(ServerLevel level, Villager villager) {
        LootTable lootTable = level.getServer().getLootData().getLootTable(BuiltInLootTables.FISHING);
        LootParams lootParams = new LootParams.Builder(level)
            .withParameter(LootContextParams.ORIGIN, villager.position())
            .withParameter(LootContextParams.TOOL, rod)
            .withLuck(0)
            .create(LootContextParamSets.FISHING);

        List<ItemStack> loot = lootTable.getRandomItems(lootParams);
        for (ItemStack stack : loot) {
            if (!InventoryHelper.tryAddToInventory(villager.getInventory(), stack)) {
                villager.spawnAtLocation(stack);
            }
        }

        level.playSound(null, villager.blockPosition(), SoundEvents.FISHING_BOBBER_RETRIEVE, SoundSource.NEUTRAL, 1.0F, 0.4F / (level.random.nextFloat() * 0.4F + 0.8F));
    }

    private BlockPos findBestStandingSpot(ServerLevel level, Villager villager) {
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;
        int searchRadius = (int) CommonConfig.scanRadius;

        for (int xOffset = -searchRadius; xOffset <= searchRadius; xOffset++) {
            for (int zOffset = -searchRadius; zOffset <= searchRadius; zOffset++) {
                for (int yOffset = -searchRadius; yOffset <= searchRadius; yOffset++) {
                    BlockPos candidate = fishingSpot.offset(xOffset, yOffset, zOffset);
                    if (!level.getBlockState(candidate.below()).isSolid()) {
                        continue;
                    }
                    if (!level.getBlockState(candidate).isAir()) {
                        continue;
                    }
                    if (!this.workplace.containsPosition(candidate)) {
                        continue;
                    }

                    double distance = candidate.distSqr(fishingSpot);
                    if (distance < bestDistance) {
                        bestDistance = distance;
                        best = candidate;
                    }
                }
            }
        }

        return best;
    }
}
