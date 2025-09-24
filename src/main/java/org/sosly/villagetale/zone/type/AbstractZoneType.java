package org.sosly.villagetale.zone.type;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import org.sosly.villagetale.api.IZoneShape;
import org.sosly.villagetale.api.IZoneType;

public abstract class AbstractZoneType implements IZoneType {
    
    @Override
    public void initialize(Level level, IZoneShape shape) {
    }
    
    @Override
    public CompoundTag serializeNBT() {
        return null;
    }
    
    @Override
    public void deserializeNBT(CompoundTag nbt) {
    }
}