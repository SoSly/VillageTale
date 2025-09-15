package org.sosly.villageworks.entity;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.mojang.serialization.Dynamic;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.entity.schedule.Schedule;
import net.minecraft.world.level.Level;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import org.jetbrains.annotations.NotNull;
import org.sosly.villageworks.VillageWorks;
import org.sosly.villageworks.data.LivingEntityFoodData;
import org.sosly.villageworks.entity.ai.behavior.VillagerGoalPackages;
import org.sosly.villageworks.registry.MemoryModuleTypes;
import org.sosly.villageworks.registry.SensorTypes;

import javax.annotation.Nullable;

public class Villager extends PathfinderMob {
    private final LivingEntityFoodData foodData;
    private final SimpleContainer inventory;
    
    private static final ImmutableList<MemoryModuleType<?>> MEMORY_TYPES = ImmutableList.of(
        MemoryModuleType.HOME,
        MemoryModuleType.NEAREST_LIVING_ENTITIES,
        MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES,
        MemoryModuleType.NEAREST_PLAYERS,
        MemoryModuleType.NEAREST_VISIBLE_PLAYER,
        MemoryModuleType.WALK_TARGET,
        MemoryModuleType.LOOK_TARGET,
        MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE,
        MemoryModuleType.PATH,
        MemoryModuleType.HURT_BY,
        MemoryModuleType.HURT_BY_ENTITY,
        MemoryModuleType.NEAREST_HOSTILE,
        MemoryModuleType.LAST_SLEPT,
        MemoryModuleType.LAST_WOKEN,
        MemoryModuleTypes.CAN_EAT.get(),
        MemoryModuleTypes.IS_HUNGRY.get(),
        MemoryModuleTypes.IS_STARVING.get()
    );

    private static final ImmutableList<SensorType<? extends Sensor<? super Villager>>> SENSOR_TYPES = ImmutableList.of(
        SensorType.NEAREST_LIVING_ENTITIES,
        SensorType.NEAREST_PLAYERS,
        SensorType.HURT_BY,
        SensorType.VILLAGER_HOSTILES,
        SensorTypes.HUNGER.get()
    );

    public Villager(EntityType<? extends Villager> entityType, Level level) {
        super(entityType, level);
        this.foodData = new LivingEntityFoodData();
        this.inventory = new SimpleContainer(1);
        ((GroundPathNavigation) this.getNavigation()).setCanOpenDoors(true);
        this.getNavigation().setCanFloat(true);
        this.setCanPickUpLoot(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
            .add(Attributes.MOVEMENT_SPEED, 0.5D)
            .add(Attributes.FOLLOW_RANGE, 48.0D)
            .add(Attributes.MAX_HEALTH, 20.0D);
    }

    @Override
    protected Brain.@NotNull Provider<Villager> brainProvider() {
        return Brain.provider(MEMORY_TYPES, SENSOR_TYPES);
    }

    @Override
    protected @NotNull Brain<?> makeBrain(@NotNull Dynamic<?> dynamic) {
        Brain<Villager> brain = this.brainProvider().makeBrain(dynamic);
        this.registerBrainGoals(brain);
        return brain;
    }

    @SuppressWarnings("unchecked")
    public @NotNull Brain<Villager> getBrain() {
        return (Brain<Villager>) super.getBrain();
    }

    public void refreshBrain(ServerLevel serverLevel) {
        Brain<Villager> brain = this.getBrain();
        brain.stopAll(serverLevel, this);
        this.brain = brain.copyWithoutBehaviors();
        this.registerBrainGoals(this.getBrain());
    }

    private void registerBrainGoals(Brain<Villager> brain) {
        brain.setSchedule(Schedule.VILLAGER_DEFAULT);

        brain.addActivity(Activity.CORE, VillagerGoalPackages.getCorePackage());
        brain.addActivity(Activity.IDLE, VillagerGoalPackages.getIdlePackage(0.3F));
        brain.addActivity(Activity.REST, VillagerGoalPackages.getRestPackage(0.6F));
        brain.addActivity(Activity.PANIC, VillagerGoalPackages.getPanicPackage(0.6F));

        brain.setCoreActivities(ImmutableSet.of(Activity.CORE));
        brain.setDefaultActivity(Activity.IDLE);
        brain.setActiveActivityIfPossible(Activity.IDLE);
        brain.updateActivityFromSchedule(this.level().getDayTime(), this.level().getGameTime());
    }

    @Override
    protected void customServerAiStep() {
        this.level().getProfiler().push("villageWorksVillagerBrain");
        this.getBrain().tick((ServerLevel) this.level(), this);
        this.level().getProfiler().pop();
        this.foodData.tick(this);
        super.customServerAiStep();
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        this.foodData.addAdditionalSaveData(tag);
        ItemStack inventoryItem = this.inventory.getItem(0);
        if (!inventoryItem.isEmpty()) {
            CompoundTag itemTag = new CompoundTag();
            inventoryItem.save(itemTag);
            tag.put("InventoryItem", itemTag);
        }
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.foodData.readAdditionalSaveData(tag);
        if (tag.contains("InventoryItem")) {
            CompoundTag itemTag = tag.getCompound("InventoryItem");
            ItemStack inventoryItem = ItemStack.of(itemTag);
            this.inventory.setItem(0, inventoryItem);
        }
        if (this.level() instanceof ServerLevel) {
            this.refreshBrain((ServerLevel) this.level());
        }
    }

    @Override
    public void setLastHurtByMob(@Nullable LivingEntity livingEntity) {
        if (livingEntity != null && this.level() instanceof ServerLevel) {
            VillageWorks.LOGGER.info("Villager {} was hurt by {}", this, livingEntity);
        }
        super.setLastHurtByMob(livingEntity);
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    @Nullable
    protected SoundEvent getAmbientSound() {
        return SoundEvents.VILLAGER_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(@NotNull DamageSource damageSource) {
        return SoundEvents.VILLAGER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.VILLAGER_DEATH;
    }

    @Override
    public void startSleeping(@NotNull BlockPos pos) {
        super.startSleeping(pos);
        this.brain.setMemory(MemoryModuleType.LAST_SLEPT, this.level().getGameTime());
        this.brain.eraseMemory(MemoryModuleType.WALK_TARGET);
        this.brain.eraseMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
    }

    public void setHome(BlockPos pos) {
        if (pos != null) {
            BlockPos bedHeadPos = getBedHeadPosition(pos);
            this.brain.setMemory(MemoryModuleType.HOME, GlobalPos.of(this.level().dimension(), bedHeadPos));
            VillageWorks.LOGGER.info("Villager {} assigned home at {} (bed head: {})", this, pos, bedHeadPos);
        } else {
            this.brain.eraseMemory(MemoryModuleType.HOME);
            VillageWorks.LOGGER.info("Villager {} home assignment cleared", this);
        }
    }

    public BlockPos getHome() {
        return this.brain.getMemory(MemoryModuleType.HOME)
            .map(GlobalPos::pos)
            .orElse(null);
    }

    public LivingEntityFoodData getFoodData() {
        return this.foodData;
    }

    public SimpleContainer getInventory() {
        return this.inventory;
    }

    @Override
    public @NotNull InteractionResult mobInteract(@NotNull Player player, @NotNull InteractionHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);
        
        if (itemStack.isEmpty()) {
            return InteractionResult.PASS;
        }
        
        if (!this.inventory.getItem(0).isEmpty()) {
            return InteractionResult.PASS;
        }
        
        FoodProperties foodProperties = itemStack.getFoodProperties(this);
        if (foodProperties == null || foodProperties.getSaturationModifier() <= 0) {
            return InteractionResult.PASS;
        }
        
        ItemStack singleItem = itemStack.copy();
        singleItem.setCount(1);
        this.inventory.setItem(0, singleItem);
        
        if (!player.getAbilities().instabuild) {
            itemStack.shrink(1);
        }
        
        this.playSound(SoundEvents.ITEM_PICKUP, 1.0F, 1.0F);
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void dropCustomDeathLoot(@NotNull DamageSource damageSource, int looting, boolean recentlyHit) {
        super.dropCustomDeathLoot(damageSource, looting, recentlyHit);
        
        ItemStack inventoryItem = this.inventory.getItem(0);
        if (!inventoryItem.isEmpty()) {
            ItemEntity itemEntity = new ItemEntity(this.level(), this.getX(), this.getY(), this.getZ(), inventoryItem);
            this.level().addFreshEntity(itemEntity);
        }
    }

    private BlockPos getBedHeadPosition(BlockPos bedPos) {
        BlockState blockState = this.level().getBlockState(bedPos);
        if (!(blockState.getBlock() instanceof BedBlock)) {
            return bedPos;
        }
        
        BedPart bedPart = blockState.getValue(BedBlock.PART);
        if (bedPart != BedPart.FOOT) {
            return bedPos;
        }
        
        return bedPos.relative(blockState.getValue(BedBlock.FACING));
    }
}
