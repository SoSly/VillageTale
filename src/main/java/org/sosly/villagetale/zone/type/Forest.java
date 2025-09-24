package org.sosly.villagetale.zone.type;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.sosly.villagetale.VillageTale;

public class Forest extends AbstractZoneType {
    public static final ResourceLocation ID = new ResourceLocation(VillageTale.MOD_ID, "forest");

    @Override
    public ResourceLocation getID() {
        return ID;
    }

    @Override
    public boolean isPOI(Level level, BlockPos pos) {
        return true;
    }
}
