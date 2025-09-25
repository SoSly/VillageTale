package org.sosly.villagetale.entity;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Dynamic;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.memory.ExpirableValue;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.InventoryCarrier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.sosly.villagetale.VillageTale;
import org.sosly.villagetale.api.IProfession;
import org.sosly.villagetale.api.capability.IVillageCapability;
import org.sosly.villagetale.api.capability.IVillagesCapability;
import org.sosly.villagetale.capability.Capabilities;
import org.sosly.villagetale.data.LivingEntityFoodData;
import org.sosly.villagetale.data.VillageInfo;
import org.sosly.villagetale.entity.ai.SensorTypes;
import org.sosly.villagetale.entity.ai.goals.VillagerGoalPackages;
import org.sosly.villagetale.entity.ai.navigation.VillagerNavigation;
import org.sosly.villagetale.helper.InventoryHelper;
import org.sosly.villagetale.network.NetworkHandler;
import org.sosly.villagetale.profession.ProfessionRegistry;
import org.sosly.villagetale.profession.professions.Commoner;

public class Villager extends PathfinderMob implements InventoryCarrier {
    private static final EntityDataAccessor<Byte> DATA_VILLAGER_ARM_POSE = SynchedEntityData.defineId(Villager.class, EntityDataSerializers.BYTE);

    private final LivingEntityFoodData foodData;
    private final SimpleContainer inventory;
    private ImmutableList<MemoryModuleType<?>> memoryTypes;

    private ImmutableList<SensorType<? extends Sensor<? super Villager>>> sensorTypes;
    private VillagerArmPose armPose = VillagerArmPose.NEUTRAL;

    public static enum VillagerArmPose {
        CROSSED,
        ATTACKING,
        SPELLCASTING,
        BOW_AND_ARROW,
        CROSSBOW_HOLD,
        CROSSBOW_CHARGE,
        CELEBRATING,
        NEUTRAL;
    }

    public Villager(EntityType<? extends Villager> entityType, Level level) {
        super(entityType, level);
        this.foodData = new LivingEntityFoodData();
        this.inventory = new SimpleContainer(5);
        ((GroundPathNavigation) this.getNavigation()).setCanOpenDoors(true);
        this.getNavigation().setCanFloat(true);
        this.setCanPickUpLoot(true);
        this.setPathfindingMalus(BlockPathTypes.DANGER_FIRE, 16.0F);
        this.setPathfindingMalus(BlockPathTypes.DAMAGE_FIRE, -1.0F);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_VILLAGER_ARM_POSE, (byte)VillagerArmPose.NEUTRAL.ordinal());
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
            .add(Attributes.MOVEMENT_SPEED, 0.5D)
            .add(Attributes.FOLLOW_RANGE, 48.0D)
            .add(Attributes.MAX_HEALTH, 20.0D);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        return new VillagerNavigation(this, level);
    }

    @Override
    protected Brain.@NotNull Provider<Villager> brainProvider() {
        if (this.memoryTypes == null) {
            this.memoryTypes = DefaultVillagerBrain.getMemoryModules();
        }
        if (this.sensorTypes == null) {
            this.sensorTypes = DefaultVillagerBrain.getSensors();
        }
        return Brain.provider(this.memoryTypes, this.sensorTypes);
    }

    @Override
    protected @NotNull Brain<?> makeBrain(@NotNull Dynamic<?> dynamic) {
        Brain<Villager> brain = this.brainProvider().makeBrain(dynamic);
        return brain;
    }

    @SuppressWarnings("unchecked")
    public @NotNull Brain<Villager> getBrain() {
        return (Brain<Villager>) super.getBrain();
    }

    public void rebuildBrainWithProfession() {
        Map<MemoryModuleType<?>, Optional<? extends ExpirableValue<?>>> savedMemories =
                new HashMap<>(this.brain.getMemories());

        this.memoryTypes = ImmutableList.<MemoryModuleType<?>>builder()
            .addAll(DefaultVillagerBrain.getMemoryModules())
            .addAll(this.getProfession().getMemoryModules())
            .build();
        this.sensorTypes = ImmutableList.<SensorType<? extends Sensor<? super Villager>>>builder()
            .addAll(DefaultVillagerBrain.getSensors())
            .addAll(this.getProfession().getSensors())
            .build();

        NbtOps nbtops = NbtOps.INSTANCE;
        this.brain = this.makeBrain(new Dynamic<>(nbtops, nbtops.createMap(ImmutableMap.of(nbtops.createString("memories"), nbtops.emptyMap()))));

        for(Map.Entry<MemoryModuleType<?>, Optional<? extends ExpirableValue<?>>> entry : savedMemories.entrySet()) {
            if (entry.getValue().isPresent() && this.brain.getMemories().containsKey(entry.getKey())) {
                this.brain.getMemories().put(entry.getKey(), entry.getValue());
            }
        }
    }

    public void refreshBrain(ServerLevel serverLevel) {
        this.getBrain().stopAll(serverLevel, this);
        this.rebuildBrainWithProfession();
        this.registerBrainGoals(this.getBrain());
    }

    private void registerBrainGoals(Brain<Villager> brain) {
        brain.setSchedule(this.getProfession().getSchedule());

        brain.addActivity(Activity.CORE, VillagerGoalPackages.getCorePackage());
        brain.addActivity(Activities.MORNING_IDLE.get(), VillagerGoalPackages.getMorningIdlePackage(0.3F));
        brain.addActivity(Activities.EVENING_IDLE.get(), VillagerGoalPackages.getEveningIdlePackage(0.3F));
        brain.addActivity(Activity.REST, VillagerGoalPackages.getRestPackage(0.6F));
        brain.addActivity(Activity.PANIC, VillagerGoalPackages.getPanicPackage(0.6F));

        brain.addActivity(Activity.WORK, getCombinedWorkPackage());

        brain.setCoreActivities(ImmutableSet.of(Activity.CORE));
        brain.setDefaultActivity(Activities.MORNING_IDLE.get());
        brain.setActiveActivityIfPossible(Activities.MORNING_IDLE.get());
        brain.updateActivityFromSchedule(this.level().getDayTime(), this.level().getGameTime());
    }

    private ImmutableList<Pair<Integer, ? extends BehaviorControl<? super Villager>>> getCombinedWorkPackage() {
        ImmutableList.Builder<Pair<Integer, ? extends BehaviorControl<? super Villager>>> builder = ImmutableList.builder();
        builder.addAll(VillagerGoalPackages.getWorkPackage(0.3F));
        builder.addAll(this.getProfession().getWorkPackage(0.3F));
        return builder.build();
    }

    @Override
    protected void customServerAiStep() {
        this.level().getProfiler().push("villageTaleVillagerBrain");
        this.getBrain().tick((ServerLevel) this.level(), this);
        this.level().getProfiler().pop();
        this.foodData.tick(this);
        super.customServerAiStep();
    }

    public VillagerArmPose getArmPose() {
        if (this.level().isClientSide()) {
            return VillagerArmPose.values()[this.entityData.get(DATA_VILLAGER_ARM_POSE)];
        }
        return this.armPose;
    }

    public void setArmPose(VillagerArmPose pose) {
        this.armPose = pose;
        if (!this.level().isClientSide()) {
            this.entityData.set(DATA_VILLAGER_ARM_POSE, (byte)pose.ordinal());
        }
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        this.foodData.addAdditionalSaveData(tag);
        ListTag inventoryListTag = new ListTag();
        for (int i = 0; i < this.inventory.getContainerSize(); i++) {
            ItemStack itemStack = this.inventory.getItem(i);
            if (!itemStack.isEmpty()) {
                CompoundTag itemTag = new CompoundTag();
                itemTag.putByte("Slot", (byte) i);
                itemStack.save(itemTag);
                inventoryListTag.add(itemTag);
            }
        }
        if (!inventoryListTag.isEmpty()) {
            tag.put("Inventory", inventoryListTag);
        }

        Optional<UUID> villageId = this.brain.getMemory(MemoryModuleTypes.VILLAGE.get());
        if (villageId.isPresent()) {
            tag.putString("VillageId", villageId.get().toString());
        }

        Optional<ResourceLocation> profession = this.brain.getMemory(MemoryModuleTypes.PROFESSION.get());
        if (profession.isPresent()) {
            tag.putString("Profession", profession.get().toString());
        }

        Optional<UUID> workZoneId = this.brain.getMemory(MemoryModuleTypes.WORK_ZONE.get());
        if (workZoneId.isPresent()) {
            tag.putString("WorkZoneId", workZoneId.get().toString());
        }

        Optional<UUID> homeZoneId = this.brain.getMemory(MemoryModuleTypes.HOME_ZONE.get());
        if (homeZoneId.isPresent()) {
            tag.putString("HomeZoneId", homeZoneId.get().toString());
        }
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.foodData.readAdditionalSaveData(tag);
        if (tag.contains("Inventory", Tag.TAG_LIST)) {
            ListTag inventoryListTag = tag.getList("Inventory", Tag.TAG_COMPOUND);
            for (int i = 0; i < inventoryListTag.size(); i++) {
                CompoundTag itemTag = inventoryListTag.getCompound(i);
                byte slot = itemTag.getByte("Slot");
                if (slot >= 0 && slot < this.inventory.getContainerSize()) {
                    ItemStack itemStack = ItemStack.of(itemTag);
                    this.inventory.setItem(slot, itemStack);
                }
            }
        } else if (tag.contains("InventoryItem")) {
            CompoundTag itemTag = tag.getCompound("InventoryItem");
            ItemStack inventoryItem = ItemStack.of(itemTag);
            this.inventory.setItem(0, inventoryItem);
        }

        if (tag.contains("VillageId")) {
            UUID villageId = UUID.fromString(tag.getString("VillageId"));
            this.brain.setMemoryWithExpiry(MemoryModuleTypes.VILLAGE.get(), villageId, 24000L);
        }

        ResourceLocation profession = Commoner.ID;
        if (tag.contains("Profession") && ResourceLocation.tryParse(tag.getString("Profession")) != null) {
            profession = ResourceLocation.tryParse(tag.getString("Profession"));
        }
        this.brain.setMemoryWithExpiry(MemoryModuleTypes.PROFESSION.get(), profession, 24000L);

        if (tag.contains("WorkZoneId")) {
            UUID workZoneId = UUID.fromString(tag.getString("WorkZoneId"));
            this.brain.setMemoryWithExpiry(MemoryModuleTypes.WORK_ZONE.get(), workZoneId, 24000L);
        }

        if (tag.contains("HomeZoneId")) {
            UUID homeZoneId = UUID.fromString(tag.getString("HomeZoneId"));
            this.brain.setMemoryWithExpiry(MemoryModuleTypes.HOME_ZONE.get(), homeZoneId, 24000L);
        }

        if (this.level() instanceof ServerLevel serverLevel) {
            this.refreshBrain(serverLevel);
            NetworkHandler.syncProfessionToNearbyPlayers(this);
        }
    }

    @Override
    public void setLastHurtByMob(@Nullable LivingEntity livingEntity) {
        if (livingEntity != null && this.level() instanceof ServerLevel) {
            VillageTale.LOGGER.info("Villager {} was hurt by {}", this, livingEntity);
        }
        super.setLastHurtByMob(livingEntity);
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public void remove(@NotNull RemovalReason reason) {
        clearVillage();
        super.remove(reason);
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
        VillageTale.LOGGER.info("Villager {} attempting to sleep at {}", this.getDisplayName().getString(), pos);
        super.startSleeping(pos);
        this.brain.setMemoryWithExpiry(MemoryModuleType.LAST_SLEPT, this.level().getGameTime(), 24000L);
        this.brain.eraseMemory(MemoryModuleType.WALK_TARGET);
        this.brain.eraseMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
    }

    @Override
    protected float getStandingEyeHeight(@NotNull Pose pose, @NotNull EntityDimensions dimensions) {
        return pose == Pose.SLEEPING ? 0.2F : super.getStandingEyeHeight(pose, dimensions);
    }

    public void setHome(BlockPos pos) {
        if (pos != null) {
            BlockPos bedHeadPos = getBedHeadPosition(pos);
            this.brain.setMemoryWithExpiry(MemoryModuleType.HOME, GlobalPos.of(this.level().dimension(), bedHeadPos), 24000L);
            VillageTale.LOGGER.info("Villager {} assigned home at {} (bed head: {})", this, pos, bedHeadPos);
        } else {
            this.brain.eraseMemory(MemoryModuleType.HOME);
            VillageTale.LOGGER.info("Villager {} home assignment cleared", this);
        }
    }

    public BlockPos getHome() {
        return this.brain.getMemory(MemoryModuleType.HOME)
            .map(GlobalPos::pos)
            .orElse(null);
    }

    public IProfession getProfession() {
        ResourceLocation professionId = this.brain.getMemory(MemoryModuleTypes.PROFESSION.get()).orElse(Commoner.ID);
        return ProfessionRegistry.INSTANCE.getProfession(professionId).orElse(new Commoner());
    }

    public void setProfession(ResourceLocation professionId) {
        IProfession profession = ProfessionRegistry.INSTANCE.getProfession(professionId).orElse(null);
        if (profession == null) {
            return;
        }

        // Clear all profession and work-related memories when changing profession
        this.brain.eraseMemory(MemoryModuleTypes.WORK_ZONE.get());
        this.brain.eraseMemory(MemoryModuleTypes.WORK_POS.get());
        this.brain.eraseMemory(MemoryModuleTypes.NEAREST_WORKSTATION.get());
        this.brain.eraseMemory(MemoryModuleTypes.CURRENT_RECIPE.get());
        this.brain.eraseMemory(MemoryModuleTypes.WANTED_ITEM.get());
        this.brain.eraseMemory(MemoryModuleTypes.FOUND_ITEM.get());
        this.brain.eraseMemory(MemoryModuleTypes.NEAREST_EMPTY_FARMLAND.get());
        this.brain.eraseMemory(MemoryModuleTypes.NEAREST_HARVESTABLE_CROP.get());
        this.brain.eraseMemory(MemoryModuleTypes.NEAREST_TILLABLE_SOIL.get());
        this.brain.eraseMemory(MemoryModuleTypes.BUSY.get());
        
        this.brain.setMemoryWithExpiry(MemoryModuleTypes.PROFESSION.get(), professionId, 24000L);

        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        this.refreshBrain(serverLevel);
        NetworkHandler.syncProfessionToNearbyPlayers(this);
    }

    public Optional<UUID> getVillage() {
        return this.brain.getMemory(MemoryModuleTypes.VILLAGE.get());
    }

    public void setVillage(UUID villageId) {
        if (villageId == null) {
            clearVillage();
            return;
        }

        Optional<UUID> currentVillageId = getVillage();
        if (currentVillageId.isPresent() && currentVillageId.get().equals(villageId)) {
            return;
        }

        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        IVillagesCapability villages = serverLevel.getCapability(Capabilities.VILLAGES_CAPABILITY).orElse(null);
        if (villages == null) {
            return;
        }

        if (currentVillageId.isPresent()) {
            VillageInfo oldVillage = villages.getVillageById(currentVillageId.get());
            if (oldVillage == null) {
                return;
            }

            ChunkPos oldVillageChunk = oldVillage.getVillageStartingChunk();
            LevelChunk oldChunk = serverLevel.getChunk(oldVillageChunk.x, oldVillageChunk.z);
            IVillageCapability oldVillageCapability = oldChunk.getCapability(Capabilities.VILLAGE_CAPABILITY).orElse(null);
            if (oldVillageCapability == null) {
                return;
            }

            oldVillageCapability.removeVillagerByUUID(this.getUUID());
        }

        VillageInfo newVillage = villages.getVillageById(villageId);
        if (newVillage == null) {
            return;
        }

        ChunkPos newVillageChunk = newVillage.getVillageStartingChunk();
        LevelChunk newChunk = serverLevel.getChunk(newVillageChunk.x, newVillageChunk.z);
        IVillageCapability newVillageCapability = newChunk.getCapability(Capabilities.VILLAGE_CAPABILITY).orElse(null);
        if (newVillageCapability == null) {
            return;
        }

        newVillageCapability.addVillagerByUUID(this.getUUID());
        
        // Clear all zone-related memories when changing villages
        this.brain.eraseMemory(MemoryModuleTypes.HOME_ZONE.get());
        this.brain.eraseMemory(MemoryModuleTypes.WORK_ZONE.get());
        this.brain.eraseMemory(MemoryModuleTypes.WORK_POS.get());
        this.brain.eraseMemory(net.minecraft.world.entity.ai.memory.MemoryModuleType.HOME);
        
        this.brain.setMemoryWithExpiry(MemoryModuleTypes.VILLAGE.get(), villageId, 24000L);
        VillageTale.LOGGER.info("Villager {} assigned to village {}", this.getUUID(), villageId);
    }

    public void clearVillage() {
        Optional<UUID> currentVillageId = getVillage();
        if (currentVillageId.isEmpty()) {
            return;
        }

        // Clear all village and zone-related memories
        this.brain.eraseMemory(MemoryModuleTypes.VILLAGE.get());
        this.brain.eraseMemory(MemoryModuleTypes.HOME_ZONE.get());
        this.brain.eraseMemory(MemoryModuleTypes.WORK_ZONE.get());
        this.brain.eraseMemory(MemoryModuleTypes.WORK_POS.get());
        this.brain.eraseMemory(net.minecraft.world.entity.ai.memory.MemoryModuleType.HOME);

        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        IVillagesCapability villages = serverLevel.getCapability(Capabilities.VILLAGES_CAPABILITY).orElse(null);
        if (villages == null) {
            return;
        }

        VillageInfo village = villages.getVillageById(currentVillageId.get());
        if (village == null) {
            this.brain.eraseMemory(MemoryModuleTypes.VILLAGE.get());
            return;
        }

        ChunkPos villageChunk = village.getVillageStartingChunk();
        LevelChunk chunk = serverLevel.getChunk(villageChunk.x, villageChunk.z);
        IVillageCapability villageCapability = chunk.getCapability(Capabilities.VILLAGE_CAPABILITY).orElse(null);
        if (villageCapability != null) {
            villageCapability.removeVillagerByUUID(this.getUUID());
        }

        this.brain.eraseMemory(MemoryModuleTypes.VILLAGE.get());
        VillageTale.LOGGER.info("Villager {} village assignment cleared", this.getUUID());
    }

    public LivingEntityFoodData getFoodData() {
        return this.foodData;
    }

    @Override
    public SimpleContainer getInventory() {
        return this.inventory;
    }

    @Override
    public boolean wantsToPickUp(ItemStack stack) {
        return InventoryHelper.canAddToInventory(this.inventory, stack);
    }


    @Override
    protected void dropCustomDeathLoot(@NotNull DamageSource damageSource, int looting, boolean recentlyHit) {
        super.dropCustomDeathLoot(damageSource, looting, recentlyHit);

        for (int i = 0; i < this.inventory.getContainerSize(); i++) {
            ItemStack inventoryItem = this.inventory.getItem(i);
            if (!inventoryItem.isEmpty()) {
                ItemEntity itemEntity = new ItemEntity(this.level(), this.getX(), this.getY(), this.getZ(), inventoryItem);
                this.level().addFreshEntity(itemEntity);
            }
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

    @Override
    public boolean canBeLeashed(@NotNull Player player) {
        return false;
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        NetworkHandler.syncProfessionToPlayer(this, player);
    }

    @Override
    public @NotNull Vec3 getRopeHoldPosition(float partialTicks) {
        float f = Mth.lerp(partialTicks, this.yBodyRotO, this.yBodyRot) * ((float)Math.PI / 180F);
        Vec3 vec3 = new Vec3(0.0D, this.getBoundingBox().getYsize() - 1.0D, 0.2D);
        return this.getPosition(partialTicks).add(vec3.yRot(-f));
    }

    protected static class DefaultVillagerBrain {
        protected static ImmutableList<MemoryModuleType<?>> getMemoryModules() {
            return ImmutableList.of(
                MemoryModuleType.HOME,
                MemoryModuleType.NEAREST_LIVING_ENTITIES,
                MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES,
                MemoryModuleType.NEAREST_PLAYERS,
                MemoryModuleType.NEAREST_VISIBLE_PLAYER,
                MemoryModuleType.WALK_TARGET,
                MemoryModuleType.LOOK_TARGET,
                MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE,
                MemoryModuleType.PATH,
                MemoryModuleType.DOORS_TO_CLOSE,
                MemoryModuleType.HURT_BY,
                MemoryModuleType.HURT_BY_ENTITY,
                MemoryModuleType.NEAREST_HOSTILE,
                MemoryModuleType.LAST_SLEPT,
                MemoryModuleType.LAST_WOKEN,
                MemoryModuleTypes.LAST_DAILY_EXHAUSTION.get(),
                MemoryModuleTypes.CAN_EAT.get(),
                MemoryModuleTypes.IS_HUNGRY.get(),
                MemoryModuleTypes.IS_STARVING.get(),
                MemoryModuleTypes.PROFESSION.get(),
                MemoryModuleTypes.VILLAGE.get(),
                MemoryModuleTypes.WORK_ZONE.get(),
                MemoryModuleTypes.HOME_ZONE.get(),
                MemoryModuleTypes.BUSY.get()
            );
        }

        protected static ImmutableList<SensorType<? extends Sensor<? super Villager>>> getSensors() {
            return ImmutableList.of(
                SensorType.NEAREST_LIVING_ENTITIES,
                SensorType.NEAREST_PLAYERS,
                SensorType.HURT_BY,
                SensorType.VILLAGER_HOSTILES,
                SensorTypes.IS_HUNGRY.get(),
                SensorTypes.HAS_FOOD.get(),
                SensorTypes.HAS_TOOL.get(),
                SensorTypes.HAS_RESOURCE.get(),
                SensorTypes.IS_ITEM_IN_STORAGE.get(),
                SensorTypes.HAS_ITEMS_TO_DEPOSIT.get(),
                SensorTypes.HAS_BED_IN_HOME_ZONE.get()
            );
        }
    }
}
