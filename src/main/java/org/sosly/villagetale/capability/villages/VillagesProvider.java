package org.sosly.villagetale.capability.villages;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.sosly.villagetale.api.capability.IVillagesCapability;
import org.sosly.villagetale.capability.Capabilities;

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
        return capability.serializeNBT();
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        capability.deserializeNBT(tag);
    }

    public void invalidate() {
        lazyOptional.invalidate();
    }
}
