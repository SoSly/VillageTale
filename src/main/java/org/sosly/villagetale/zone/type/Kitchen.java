package org.sosly.villagetale.zone.type;

import net.minecraft.resources.ResourceLocation;
import org.sosly.villagetale.VillageTale;

public class Kitchen extends AbstractZoneType {
    public static final ResourceLocation ID = new ResourceLocation(VillageTale.MOD_ID, "kitchen");

    @Override
    public ResourceLocation getID() {
        return ID;
    }
}
