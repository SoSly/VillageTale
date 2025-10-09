package org.sosly.villagetale.capability.village;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.sosly.villagetale.VillageTale;
import org.sosly.villagetale.api.capability.IVillageCapability;
import org.sosly.villagetale.capability.Capabilities;

public class VillageProvider implements ICapabilitySerializable<CompoundTag> {
    private final VillageCapability capability;
    private final LazyOptional<IVillageCapability> holder;

    public VillageProvider() {
        this.capability = new VillageCapability();
        this.holder = LazyOptional.of(() -> capability);
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        if (cap != Capabilities.VILLAGE_CAPABILITY){
            return LazyOptional.empty();
        }

        return holder.cast();
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = capability.serializeNBT();

        if (capability.getChunk() != null) {
            capability.getChunk().setUnsaved(false);
        }

        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        capability.deserializeNBT(tag);
    }
}
