package org.sosly.villagetale.data.zones;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import org.sosly.villagetale.api.data.ZoneShape;
import org.sosly.villagetale.api.data.ZoneType;

public class BlockPosZone extends AbstractVillageZone {

    private BlockPos blockPos;

    public BlockPosZone(UUID uuid, ZoneType type, String name, BlockPos blockPos, Level level) {
        super(uuid, type, name, level);
        this.blockPos = blockPos;
    }

    public BlockPosZone(UUID uuid, ZoneType type, String name, BlockPos blockPos) {
        super(uuid, type, name);
        this.blockPos = blockPos;
    }

    public BlockPosZone(UUID uuid, ZoneType type, int id, String name, BlockPos blockPos, Level level) {
        super(uuid, type, id, name, level);
        this.blockPos = blockPos;
    }

    public BlockPosZone(UUID uuid, ZoneType type, int id, String name, BlockPos blockPos) {
        super(uuid, type, id, name);
        this.blockPos = blockPos;
    }

    public BlockPos getBlockPos() {
        return blockPos;
    }

    public void setBlockPos(BlockPos blockPos) {
        this.blockPos = blockPos;
    }

    @Override
    public ZoneShape getShape() {
        return ZoneShape.BLOCKPOS;
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
        if (getType() == ZoneType.NONE || blockPos == null || getLevel() == null) {
            return Optional.empty();
        }

        List<BlockPos> pois = new ArrayList<>();
        if (getType().isPOI(blockPos, getLevel())) {
            pois.add(blockPos);
        }

        return Optional.of(pois);
    }


    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = super.serializeNBT();
        tag.putByte("Shape", (byte) ZoneShape.BLOCKPOS.ordinal());

        if (blockPos != null) {
            tag.putLong("BlockPos", blockPos.asLong());
        }

        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        super.deserializeNBT(tag);

        if (tag.contains("BlockPos")) {
            this.blockPos = BlockPos.of(tag.getLong("BlockPos"));
        }
    }

    @Override
    public BlockPos getStartPos() {
        return this.blockPos;
    }
}
