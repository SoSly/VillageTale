package org.sosly.villagetale.zone;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.sosly.villagetale.api.IVillageZone;
import org.sosly.villagetale.api.IZoneShape;
import org.sosly.villagetale.api.IZoneType;
import org.sosly.villagetale.api.capability.IVillageCapability;

public class Zone implements IVillageZone {
    private final Level level;

    private UUID id;
    private int ordinal;
    private IZoneShape shape;
    private IZoneType type;
    private IVillageCapability village;
    private List<UUID> villagers = new ArrayList<>();
    private Map<BlockPos, Claim> claims = new HashMap<>();
    private String name;

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
    }

    @Override
    public UUID getUUID() {
        return id;
    }

    @Override
    public String getName() {
        if (name != null) {
            return name;
        }

        String translationKey = this.type.getID().getNamespace()
                .concat(".zone.")
                .concat(this.type.getID().getPath());
        String zoneName = Component.translatable(translationKey).toString();
        return Component.translatable("villagetale.zone.numbered", translationKey, ordinal).getString();
    }

    @Override
    public void setName(String name) {
        this.name = name;
        markDirty();
    }

    public void setShape(IZoneShape shape) {
        this.shape = shape;
        markDirty();
    }

    public void setType(IZoneType type) {
        this.type = type;
        markDirty();
    }

    @Override
    public IZoneShape getShape() {
        return shape;
    }

    @Override
    public IZoneType getType() {
        return type;
    }

    @Override
    public List<UUID> getAssignedVillagers() {
        return villagers;
    }

    @Override
    public void addAssignedVillager(UUID villagerUUID) {
        if (villagers.contains(villagerUUID)) {
            return;
        }

        villagers.add(villagerUUID);
        markDirty();
    }

    @Override
    public boolean removeAssignedVillager(UUID villagerUUID) {
        if (!villagers.contains(villagerUUID)) {
            return false;
        }

        villagers.remove(villagerUUID);
        markDirty();
        return true;
    }

    @Override
    public Map<BlockPos, Optional<UUID>> getClaims(long currentTime) {
        this.claims.entrySet()
                .removeIf(entry -> entry.getValue().isExpired(currentTime));

        Optional<List<BlockPos>> pois = getPOIs();
        if (pois.isEmpty()) {
            return Map.of();
        }

        Map<BlockPos, Optional<UUID>> result = new HashMap<>();
        for (BlockPos pos : pois.get()) {
            Claim claim = this.claims.get(pos);
            result.put(pos, claim != null ? Optional.of(claim.getVillagerUUID()) : Optional.empty());
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

        List<BlockPos> available = getAvailableClaims(currentTime, Optional.empty());
        if (!available.contains(pos)) {
            return false;
        }

        long expirationTime = currentTime + durationTicks;
        claims.put(pos, new Claim(villager, expirationTime));
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
        return this.shape.containsPosition(pos);
    }

    @Override
    public BlockPos getStartPosition() {
        return this.shape.getStartPosition();
    }

    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("id", this.id);
        tag.putInt("sequence", this.ordinal);
        tag.putString("name", this.getName());
        tag.putString("type", this.type.getID().toString());

        CompoundTag typeData = this.type.serializeNBT();
        if (typeData != null) {
            tag.put("type_data", typeData);
        }

        tag.putString("shape", this.shape.getID().toString());

        CompoundTag shapeData = this.shape.serializeNBT();
        if (shapeData != null) {
            tag.put("shape_data", shapeData);
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

        return tag;
    }

    @Override
    public void deserializeNBT(IVillageCapability village, CompoundTag tag) {
        this.id = tag.getUUID("id");
        this.village = village;
        this.ordinal = tag.getInt("sequence");
        this.name = tag.getString("name");

        IZoneType type = ZoneRegistry.INSTANCE.type(new ResourceLocation(tag.getString("type")));
        if (tag.contains("type_data")) {
            type.deserializeNBT(tag.getCompound("type_data"));
        }
        this.type = type;

        IZoneShape shape = ZoneRegistry.INSTANCE.shape(new ResourceLocation(tag.getString("shape")));
        if (tag.contains("shape_data")) {
            shape.deserializeNBT(tag.getCompound("shape_data"));
        }
        this.shape = shape;

        if (tag.contains("villagers")) {
            ListTag villagersList = tag.getList("villagers", 10);
            for (int i = 0; i < villagersList.size(); i++) {
                UUID villagerUUID = villagersList.getCompound(i).getUUID("villager");
                this.villagers.add(villagerUUID);
            }
        }
    }

    private Optional<List<BlockPos>> getPOIs() {
        return Optional.ofNullable(this.shape.getPOIs(this.village.getChunk().getLevel(),
                (pos) -> this.type.isPOI(this.village.getChunk().getLevel(), pos)));
    }

    private void markDirty() {
        this.village.getChunk().setUnsaved(true);
    }
}
