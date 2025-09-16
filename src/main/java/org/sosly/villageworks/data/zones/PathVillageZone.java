package org.sosly.villageworks.data.zones;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.Tag;
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

public class PathVillageZone extends AbstractVillageZone implements IVillageZone {

    private List<BlockPos> path;
    private List<BlockPos> cachedPOIs;
    private boolean poiCacheDirty = true;

    public PathVillageZone(UUID uuid, ZoneType type, int id, String name, List<BlockPos> path, Level level) {
        super(uuid, type, id, name, level);
        this.path = new ArrayList<>(path != null ? path : new ArrayList<>());
    }

    public PathVillageZone(UUID uuid, ZoneType type, int id, String name, List<BlockPos> path) {
        super(uuid, type, id, name);
        this.path = new ArrayList<>(path != null ? path : new ArrayList<>());
    }

    public List<BlockPos> getPath() {
        return new ArrayList<>(path);
    }

    public void setPath(List<BlockPos> path) {
        this.path = new ArrayList<>(path != null ? path : new ArrayList<>());
        this.poiCacheDirty = true;
    }

    public void addPoint(BlockPos point) {
        if (point != null) {
            path.add(point);
            this.poiCacheDirty = true;
        }
    }

    public void clearPath() {
        path.clear();
        this.poiCacheDirty = true;
    }

    @Override
    public ZoneShape getShape() {
        return ZoneShape.PATH;
    }

    @Override
    public boolean containsPosition(BlockPos pos) {
        if (pos == null || path.isEmpty()) {
            return false;
        }

        return path.contains(pos);
    }

    @Override
    public Optional<List<BlockPos>> getPOIs() {
        if (getType() == ZoneType.NONE || getLevel() == null) {
            return Optional.empty();
        }

        if (poiCacheDirty) {
            rescanPOIs(getLevel());
        }

        return Optional.of(new ArrayList<>(cachedPOIs));
    }

    public void rescanPOIs(Level level) {
        if (level == null || getType() == ZoneType.NONE || path.isEmpty()) {
            cachedPOIs = new ArrayList<>();
            poiCacheDirty = false;
            return;
        }

        cachedPOIs = new ArrayList<>();

        for (BlockPos pos : path) {
            if (getType().isPOI(pos, level)) {
                cachedPOIs.add(pos);
            }
        }

        poiCacheDirty = false;
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = super.serializeNBT();
        tag.putByte("Shape", (byte) ZoneShape.PATH.ordinal());

        ListTag pathList = new ListTag();
        for (BlockPos pos : path) {
            pathList.add(LongTag.valueOf(pos.asLong()));
        }
        tag.put("Path", pathList);

        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        super.deserializeNBT(tag);

        this.path = new ArrayList<>();
        if (tag.contains("Path", Tag.TAG_LIST)) {
            ListTag pathList = tag.getList("Path", Tag.TAG_LONG);
            for (int i = 0; i < pathList.size(); i++) {
                LongTag longTag = (LongTag) pathList.get(i);
                this.path.add(BlockPos.of(longTag.getAsLong()));
            }
        }
        this.poiCacheDirty = true;
    }
}
