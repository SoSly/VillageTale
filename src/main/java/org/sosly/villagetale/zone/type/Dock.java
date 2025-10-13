package org.sosly.villagetale.zone.type;

import net.minecraft.resources.ResourceLocation;
import org.sosly.villagetale.VillageTale;

public class Dock extends AbstractZoneType {
    public static final ResourceLocation ID = new ResourceLocation(VillageTale.MOD_ID, "dock");

    @Override
    public ResourceLocation getID() {
        return ID;
    }
}
