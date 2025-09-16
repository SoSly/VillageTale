package org.sosly.villageworks.data.zones;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import org.sosly.villageworks.api.data.IVillageZone;
import org.sosly.villageworks.api.data.ZoneShape;
import org.sosly.villageworks.api.data.ZoneType;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class BlockPosVillageZone implements IVillageZone {

    private final UUID uuid;
    private final ZoneType type;
    private final int id;
    private String name;
    private BlockPos blockPos;
    private List<BlockPos> cachedPOIs;
    private boolean poiCacheDirty = true;

    public BlockPosVillageZone(UUID uuid, ZoneType type, int id, String name, BlockPos blockPos) {
        this.uuid = Objects.requireNonNull(uuid, "UUID cannot be null");
        this.type = Objects.requireNonNull(type, "Zone type cannot be null");
        this.id = id;
        this.name = name;
        this.blockPos = blockPos;
    }

    public BlockPos getBlockPos() {
        return blockPos;
    }

    public void setBlockPos(BlockPos blockPos) {
        this.blockPos = blockPos;
        this.poiCacheDirty = true;
    }

    @Override
    public UUID getUUID() {
        return uuid;
    }

    @Override
    public String getName() {
        if (name != null) {
            return name;
        }

        String translationKey = "villageworks.zone." + type.name().toLowerCase();
        String zoneName = Component.translatable(translationKey).getString();
        return Component.translatable("villageworks.zone.numbered", zoneName, id).getString();
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public ZoneShape getShape() {
        return ZoneShape.BLOCKPOS;
    }

    @Override
    public ZoneType getType() {
        return type;
    }

    @Override
    public boolean containsPosition(BlockPos pos) {
        if (blockPos == null || pos == null) {
            return false;
        }

        return blockPos.equals(pos);
    }

    @Override
    public Optional<List<BlockPos>> getPOIs() {
        if (getType() == ZoneType.NONE) {
            return Optional.empty();
        }

        if (poiCacheDirty) {
            rescanPOIs();
        }

        return Optional.of(new ArrayList<>(cachedPOIs));
    }

    private void rescanPOIs() {
        cachedPOIs = new ArrayList<>();

        if (blockPos == null) {
            poiCacheDirty = false;
            return;
        }

        poiCacheDirty = false;
    }

    public void rescanPOIs(Level level) {
        if (level == null || getType() == ZoneType.NONE || blockPos == null) {
            cachedPOIs = new ArrayList<>();
            poiCacheDirty = false;
            return;
        }

        cachedPOIs = new ArrayList<>();

        if (getType().isPOI(blockPos, level)) {
            cachedPOIs.add(blockPos);
        }

        poiCacheDirty = false;
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putString("UUID", uuid.toString());
        tag.putByte("Type", (byte) type.ordinal());
        tag.putShort("Id", (short) id);
        if (name != null) {
            tag.putString("Name", name);
        }
        tag.putByte("Shape", (byte) ZoneShape.BLOCKPOS.ordinal());

        if (blockPos != null) {
            tag.putLong("BlockPos", blockPos.asLong());
        }

        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        if (tag.contains("Name")) {
            this.name = tag.getString("Name");
        }

        if (tag.contains("BlockPos")) {
            this.blockPos = BlockPos.of(tag.getLong("BlockPos"));
        }
        this.poiCacheDirty = true;
    }
}
