package org.sosly.villagetale.capability.villages;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.sosly.villagetale.api.capability.IVillagesCapability;
import org.sosly.villagetale.capability.Capabilities;
import org.sosly.villagetale.data.VillageInfo;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class VillagesProvider implements ICapabilitySerializable<CompoundTag> {

    private final VillagesCapability capability;
    private final LazyOptional<IVillagesCapability> lazyOptional;

    public VillagesProvider(Level level) {
        this.capability = new VillagesCapability(level);
        this.lazyOptional = LazyOptional.of(() -> capability);
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        if (cap == Capabilities.VILLAGES_CAPABILITY) {
            return lazyOptional.cast();
        }
        return LazyOptional.empty();
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();

        ListTag villageList = new ListTag();
        for (VillageInfo village : capability.getVillages()) {
            villageList.add(village.serializeNBT());
        }
        tag.put("villages", villageList);

        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        if (!tag.contains("villages", Tag.TAG_LIST)) {
            return;
        }

        ListTag villageList = tag.getList("villages", Tag.TAG_COMPOUND);
        for (int i = 0; i < villageList.size(); i++) {
            CompoundTag villageTag = villageList.getCompound(i);
            VillageInfo village = VillageInfo.deserializeNBT(villageTag);
            if (village != null) {
                capability.loadVillage(village);
            }
        }
    }

    public void invalidate() {
        lazyOptional.invalidate();
    }
}
