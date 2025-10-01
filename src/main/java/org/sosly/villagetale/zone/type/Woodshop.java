package org.sosly.villagetale.zone.type;

import net.minecraft.resources.ResourceLocation;
import org.sosly.villagetale.VillageTale;

public class Woodshop extends AbstractZoneType {
    public static final ResourceLocation ID = new ResourceLocation(VillageTale.MOD_ID, "woodshop");

    @Override
    public ResourceLocation getID() {
        return ID;
    }
}
