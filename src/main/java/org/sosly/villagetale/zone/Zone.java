package org.sosly.villagetale.zone;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.sosly.villagetale.api.IVillageZone;
import org.sosly.villagetale.api.IZoneShape;
import org.sosly.villagetale.api.IZoneType;
import org.sosly.villagetale.api.capability.IVillageCapability;
import org.sosly.villagetale.network.NetworkHandler;
import org.sosly.villagetale.network.ZoneBoundaryPacket;
import net.minecraftforge.network.PacketDistributor;

public class Zone implements IVillageZone {
    private final Level level;
    private final CopyOnWriteArrayList<UUID> villagers = new CopyOnWriteArrayList<>();
    private final ConcurrentHashMap<BlockPos, Claim> claims = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Claim> entityClaims = new ConcurrentHashMap<>();
    private final CopyOnWriteArrayList<ItemStack> filter = new CopyOnWriteArrayList<>();
    private final Set<ResourceLocation> entityTypeFilter = Collections.synchronizedSet(new HashSet<>());
    private volatile UUID id;
    private volatile int ordinal;
    private volatile IZoneShape shape;
    private volatile IZoneType type;
    private volatile IVillageCapability village;
    private volatile String name;

    public Zone(Level level) {
        this.level = level;
    }

    public Zone(Level level, UUID uuid, IVillageCapability village, int ordinal) {
        this.level = level;
        this.id = uuid;
        this.village = village;
        this.ordinal = ordinal;
    }

    public Zone(Level level, UUID uuid, IVillageCapability village, int ordinal, IZoneShape shape, IZoneType type) {
        this.id = uuid;
        this.level = level;
        this.shape = shape;
        this.type = type;
        this.ordinal = ordinal;
        this.village = village;

        if (type != null && level != null) {
            type.initialize(level, shape);
        }
    }

    @Override
    public UUID getUUID() {
        return id;
    }

    @Override
    public String getName() {
        String currentName = name;
        if (currentName != null) {
            return currentName;
        }

        IZoneType currentType = type;
        if (currentType == null) {
            return "";
        }

        String translationKey = currentType.getID().getNamespace()
                .concat(".zone.")
                .concat(currentType.getID().getPath());
        return Component.translatable("villagetale.zone.numbered", translationKey, ordinal).getString();
    }

    @Override
    public synchronized void setName(String name) {
        this.name = name;
        markDirty();
    }

    @Override
    public IZoneShape getShape() {
        return shape;
    }

    public synchronized void setShape(IZoneShape shape) {
        this.shape = shape;
        markDirty();
    }

    @Override
    public IZoneType getType() {
        return type;
    }

    public synchronized void setType(IZoneType type) {
        this.type = type;
        markDirty();
    }

    @Override
    public List<UUID> getAssignedVillagers() {
        return Collections.unmodifiableList(new ArrayList<>(villagers));
    }

    @Override
    public void addAssignedVillager(UUID villagerUUID) {
        if (villagerUUID == null || villagers.contains(villagerUUID)) {
            return;
        }

        villagers.addIfAbsent(villagerUUID);
        markDirty();
    }

    @Override
    public boolean removeAssignedVillager(UUID villagerUUID) {
        if (villagerUUID == null) {
            return false;
        }

        boolean removed = villagers.remove(villagerUUID);
        if (removed) {
            markDirty();
        }

        return removed;
    }

    @Override
    public Map<BlockPos, Optional<UUID>> getClaims(long currentTime) {
        // Clean expired claims atomically
        claims.entrySet().removeIf(entry -> entry.getValue().isExpired(currentTime));

        Optional<List<BlockPos>> pois = getPOIs();
        if (pois.isEmpty()) {
            return Map.of();
        }

        Map<BlockPos, Optional<UUID>> result = new HashMap<>();
        for (BlockPos pos : pois.get()) {
            Claim claim = claims.get(pos);
            if (claim == null || claim.isExpired(currentTime)) {
                result.put(pos, Optional.empty());
                continue;
            }

            result.put(pos, Optional.of(claim.getVillagerUUID()));
        }

        return result;
    }

    @Override
    public Map<BlockPos, UUID> getActiveClaims(long currentTime, Optional<Predicate<BlockState>> blockFilter) {
        return getClaims(currentTime).entrySet().stream()
                .filter(entry -> entry.getValue().isPresent())
                .filter(claim -> blockFilter
                        .map(filter -> filter.test(this.village.getChunk().getLevel().getBlockState(claim.getKey())))
                        .orElse(true))
                .flatMap(entry -> entry.getValue().map(uuid -> Map.entry(entry.getKey(), uuid)).stream())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    @Override
    public List<BlockPos> getAvailableClaims(long currentTime, Optional<Predicate<BlockState>> blockFilter) {
        return getClaims(currentTime).entrySet().stream()
                .filter(claim -> claim.getValue().isEmpty())
                .filter(claim -> blockFilter
                        .map(filter -> filter.test(this.village.getChunk().getLevel().getBlockState(claim.getKey())))
                        .orElse(true))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    @Override
    public boolean claim(BlockPos pos, UUID villager, int durationTicks, long currentTime) {
        if (pos == null || villager == null) {
            return false;
        }

        if (!containsPosition(pos)) {
            return false;
        }

        long expirationTime = currentTime + durationTicks;
        Claim newClaim = new Claim(villager, expirationTime);

        boolean[] claimed = {false};
        claims.compute(pos, (key, existingClaim) -> {
            if (existingClaim != null && !existingClaim.isExpired(currentTime)) {
                return existingClaim;
            }

            claimed[0] = true;
            return newClaim;
        });

        if (!claimed[0]) {
            return false;
        }

        if (durationTicks > 6000) {
            markDirty();
        }

        return true;
    }

    @Override
    public boolean release(BlockPos pos) {
        return claims.remove(pos) != null;
    }

    @Override
    public boolean containsPosition(BlockPos pos) {
        IZoneShape currentShape = shape;
        return currentShape != null && currentShape.containsPosition(pos);
    }

    @Override
    public boolean containsPosition(BlockPos pos, int buffer) {
        IZoneShape currentShape = shape;
        return currentShape != null && currentShape.containsPosition(pos, buffer);
    }

    @Override
    public BlockPos getStartPosition() {
        IZoneShape currentShape = shape;
        return currentShape != null ? currentShape.getStartPosition() : null;
    }

    @Override
    public boolean claim(UUID entityId, UUID villagerUUID, int durationTicks, long currentTime) {
        if (entityId == null || villagerUUID == null) {
            return false;
        }

        long expirationTime = currentTime + durationTicks;
        Claim newClaim = new Claim(villagerUUID, expirationTime);

        boolean[] claimed = {false};
        entityClaims.compute(entityId, (key, existingClaim) -> {
            if (existingClaim != null && !existingClaim.isExpired(currentTime)) {
                return existingClaim;
            }

            claimed[0] = true;
            return newClaim;
        });

        if (!claimed[0]) {
            return false;
        }

        if (durationTicks > 6000) {
            markDirty();
        }

        return true;
    }

    @Override
    public boolean release(UUID entityId) {
        return entityClaims.remove(entityId) != null;
    }

    @Override
    public Map<UUID, Optional<UUID>> getEntityClaims(long currentTime) {
        entityClaims.entrySet().removeIf(entry -> entry.getValue().isExpired(currentTime));

        Map<UUID, Optional<UUID>> result = new HashMap<>();
        for (Map.Entry<UUID, Claim> entry : entityClaims.entrySet()) {
            if (entry.getValue().isExpired(currentTime)) {
                continue;
            }

            result.put(entry.getKey(), Optional.of(entry.getValue().getVillagerUUID()));
        }

        return result;
    }

    @Override
    public Map<UUID, UUID> getActiveEntityClaims(long currentTime) {
        entityClaims.entrySet().removeIf(entry -> entry.getValue().isExpired(currentTime));

        Map<UUID, UUID> result = new HashMap<>();
        for (Map.Entry<UUID, Claim> entry : entityClaims.entrySet()) {
            if (entry.getValue().isExpired(currentTime)) {
                continue;
            }

            result.put(entry.getKey(), entry.getValue().getVillagerUUID());
        }

        return result;
    }

    @Override
    public List<UUID> getAvailableEntityClaims(long currentTime) {
        entityClaims.entrySet().removeIf(entry -> entry.getValue().isExpired(currentTime));
        return new ArrayList<>();
    }

    @Override
    public List<ItemStack> getFilter() {
        return Collections.unmodifiableList(new ArrayList<>(filter));
    }

    @Override
    public void setFilter(List<ItemStack> filter) {
        this.filter.clear();
        if (filter != null) {
            this.filter.addAll(filter);
        }
        markDirty();
    }

    @Override
    public Set<ResourceLocation> getEntityTypeFilter() {
        return Collections.unmodifiableSet(new HashSet<>(entityTypeFilter));
    }

    @Override
    public void setEntityTypeFilter(Set<ResourceLocation> entityTypes) {
        this.entityTypeFilter.clear();
        if (entityTypes != null) {
            this.entityTypeFilter.addAll(entityTypes);
        }
        markDirty();
    }

    public synchronized CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("id", this.id);
        tag.putInt("sequence", this.ordinal);
        tag.putString("name", this.getName());

        IZoneType currentType = this.type;
        if (currentType != null) {
            tag.putString("type", currentType.getID().toString());
            CompoundTag typeData = currentType.serializeNBT();
            if (typeData != null) {
                tag.put("type_data", typeData);
            }
        }

        IZoneShape currentShape = this.shape;
        if (currentShape != null) {
            tag.putString("shape", currentShape.getID().toString());
            CompoundTag shapeData = currentShape.serializeNBT();
            if (shapeData != null) {
                tag.put("shape_data", shapeData);
            }
        }

        if (!villagers.isEmpty()) {
            ListTag villagersList = new ListTag();
            for (UUID villagerUUID : villagers) {
                CompoundTag villagerTag = new CompoundTag();
                villagerTag.putUUID("villager", villagerUUID);
                villagersList.add(villagerTag);
            }
            tag.put("villagers", villagersList);
        }

        if (!filter.isEmpty()) {
            ListTag filterList = new ListTag();
            for (ItemStack stack : filter) {
                CompoundTag itemTag = stack.save(new CompoundTag());
                filterList.add(itemTag);
            }
            tag.put("filter", filterList);
        }

        if (!entityTypeFilter.isEmpty()) {
            ListTag entityTypeList = new ListTag();
            for (ResourceLocation entityType : entityTypeFilter) {
                CompoundTag entityTypeTag = new CompoundTag();
                entityTypeTag.putString("id", entityType.toString());
                entityTypeList.add(entityTypeTag);
            }
            tag.put("entity_type_filter", entityTypeList);
        }

        // Only persist long-duration claims
        long currentTime = System.currentTimeMillis();
        Map<BlockPos, Claim> persistableClaims = new HashMap<>();
        claims.forEach((pos, claim) -> {
            if (claim.getExpirationTime() - currentTime > 6000) {
                persistableClaims.put(pos, claim);
            }
        });

        if (!persistableClaims.isEmpty()) {
            ListTag claimsList = new ListTag();
            for (Map.Entry<BlockPos, Claim> entry : persistableClaims.entrySet()) {
                CompoundTag claimTag = new CompoundTag();
                claimTag.putLong("X", entry.getKey().getX());
                claimTag.putLong("Y", entry.getKey().getY());
                claimTag.putLong("Z", entry.getKey().getZ());
                claimTag.put("Claim", entry.getValue().serializeNBT());
                claimsList.add(claimTag);
            }
            tag.put("claims", claimsList);
        }

        Map<UUID, Claim> persistableEntityClaims = new HashMap<>();
        entityClaims.forEach((entityId, claim) -> {
            if (claim.getExpirationTime() - currentTime > 6000) {
                persistableEntityClaims.put(entityId, claim);
            }
        });

        if (!persistableEntityClaims.isEmpty()) {
            ListTag entityClaimsList = new ListTag();
            for (Map.Entry<UUID, Claim> entry : persistableEntityClaims.entrySet()) {
                CompoundTag claimTag = new CompoundTag();
                claimTag.putUUID("EntityId", entry.getKey());
                claimTag.put("Claim", entry.getValue().serializeNBT());
                entityClaimsList.add(claimTag);
            }
            tag.put("entity_claims", entityClaimsList);
        }

        return tag;
    }

    @Override
    public synchronized void deserializeNBT(IVillageCapability village, CompoundTag tag) {
        this.id = tag.getUUID("id");
        this.village = village;
        this.ordinal = tag.getInt("sequence");
        this.name = tag.getString("name");

        if (tag.contains("type")) {
            IZoneType type = ZoneRegistry.INSTANCE.type(new ResourceLocation(tag.getString("type")));
            if (tag.contains("type_data")) {
                type.deserializeNBT(tag.getCompound("type_data"));
            }
            this.type = type;
        }

        if (tag.contains("shape")) {
            IZoneShape shape = ZoneRegistry.INSTANCE.shape(new ResourceLocation(tag.getString("shape")));
            if (tag.contains("shape_data")) {
                shape.deserializeNBT(tag.getCompound("shape_data"));
            }
            this.shape = shape;
        }

        villagers.clear();
        if (tag.contains("villagers")) {
            ListTag villagersList = tag.getList("villagers", 10);
            for (int i = 0; i < villagersList.size(); i++) {
                UUID villagerUUID = villagersList.getCompound(i).getUUID("villager");
                villagers.add(villagerUUID);
            }
        }

        filter.clear();
        if (tag.contains("filter")) {
            ListTag filterList = tag.getList("filter", 10);
            for (int i = 0; i < filterList.size(); i++) {
                CompoundTag itemTag = filterList.getCompound(i);
                ItemStack stack = ItemStack.of(itemTag);
                if (stack.isEmpty()) {
                    continue;
                }

                filter.add(stack);
            }
        }

        entityTypeFilter.clear();
        if (tag.contains("entity_type_filter")) {
            ListTag entityTypeList = tag.getList("entity_type_filter", 10);
            for (int i = 0; i < entityTypeList.size(); i++) {
                CompoundTag entityTypeTag = entityTypeList.getCompound(i);
                ResourceLocation entityType = new ResourceLocation(entityTypeTag.getString("id"));
                entityTypeFilter.add(entityType);
            }
        }

        claims.clear();
        if (tag.contains("claims")) {
            ListTag claimsList = tag.getList("claims", 10);
            for (int i = 0; i < claimsList.size(); i++) {
                CompoundTag claimTag = claimsList.getCompound(i);
                BlockPos pos = new BlockPos(
                        claimTag.getInt("X"),
                        claimTag.getInt("Y"),
                        claimTag.getInt("Z")
                );
                Optional<Claim> claim = Claim.deserializeNBT(claimTag.getCompound("Claim"));
                claim.ifPresent(c -> claims.put(pos, c));
            }
        }

        entityClaims.clear();
        if (tag.contains("entity_claims")) {
            ListTag entityClaimsList = tag.getList("entity_claims", 10);
            for (int i = 0; i < entityClaimsList.size(); i++) {
                CompoundTag claimTag = entityClaimsList.getCompound(i);
                UUID entityId = claimTag.getUUID("EntityId");
                Optional<Claim> claim = Claim.deserializeNBT(claimTag.getCompound("Claim"));
                claim.ifPresent(c -> entityClaims.put(entityId, c));
            }
        }
    }

    private Optional<List<BlockPos>> getPOIs() {
        IVillageCapability currentVillage = village;
        IZoneShape currentShape = shape;
        IZoneType currentType = type;

        if (currentVillage == null || currentShape == null || currentType == null) {
            return Optional.empty();
        }

        return Optional.ofNullable(currentShape.getPOIs(currentVillage.getChunk().getLevel(),
                (pos) -> currentType.isPOI(currentVillage.getChunk().getLevel(), pos)));
    }

    private void markDirty() {
        IVillageCapability currentVillage = village;
        if (currentVillage == null || currentVillage.getChunk() == null) {
            return;
        }

        currentVillage.getChunk().setUnsaved(true);

        if (level == null || level.isClientSide || shape == null) {
            return;
        }

        ZoneBoundaryPacket packet = shape.createBoundaryPacket(id, currentVillage.getUUID());
        if (packet == null) {
            return;
        }

        NetworkHandler.CHANNEL.send(
            PacketDistributor.DIMENSION.with(() -> level.dimension()),
            packet
        );
    }
}
